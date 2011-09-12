/*
 * Copyright 2011 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.dogmaticmvc.core.plugins.phases;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.kantega.dogmaticmvc.Coverage;
import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.instrumentation.CodeLine;
import org.kantega.dogmaticmvc.instrumentation.InstrumentingClassVisitor;
import org.kantega.dogmaticmvc.instrumentation.LineVisitorRegistry;
import org.kantega.dogmaticmvc.instrumentation.MaxLineNumberClassVisitor;
import org.kantega.dogmaticmvc.testrun.RequestMethodRunner;
import org.kantega.dogmaticmvc.api.VerificationPhase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static org.kantega.dogmaticmvc.web.DogmaticMVCHandler.*;

/**
 *
 */
public class SufficientTestCoverage implements VerificationPhase {
    private final ServletContext servletContext;
    private final TemplateEngine templateEngine;

    public SufficientTestCoverage(ServletContext servletContext, TemplateEngine templateEngine) {
        this.servletContext = servletContext;
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {
        Map<String, byte[]> registryBytes = new HashMap<String, byte[]>();

        final BytesMapClassLoader registryClassLoader = new BytesMapClassLoader(getClass().getClassLoader(), registryBytes);

        {
            final String lvr = LineVisitorRegistry.class.getName();
            final URL resource = LineVisitorRegistry.class.getClassLoader().getResource(lvr.replace('.', '/') + ".class");
            try {
                registryBytes.put(lvr, toBytes(resource.openStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        final Class instrumentedClassUndertest;

        final Class<?> lvrClass;

        try {


            lvrClass = registryClassLoader.loadClass(LineVisitorRegistry.class.getName());


            final BytesMapClassLoader instrumentationLoader = new BytesMapClassLoader(registryClassLoader, instrumentCoverage(compiledBytes, lvrClass));


            instrumentedClassUndertest = instrumentationLoader.loadClass(method.getDeclaringClass().getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Method instrumentedMethod = findSameMethod(method, instrumentedClassUndertest);

        Class instrumentedTestClass = getTestClass(instrumentedMethod);

        try {
            final RequestMethodRunner runner = new RequestMethodRunner(instrumentedTestClass);
            runner.run(new RunNotifier());
        } catch (InitializationError initializationError) {
            throw new RuntimeException(initializationError);
        }
        try {

            final Field lcField = lvrClass.getField("lineCounts");
            int[][] lineCounts = (int[][]) lcField.get(null);

            final Field cnField = lvrClass.getField("classNames");
            String[] classNames = (String[]) cnField.get(null);

            for (int i = 0; i < classNames.length && classNames[i] != null; i++) {
                String className = classNames[i];

                if (className.equals(instrumentedClassUndertest.getName())) {
                    int[] lineCoverage = lineCounts[i];
                    int numLines = 0;
                    int coveredLines = 0;

                    for (int j = 0; j < lineCoverage.length; j++) {
                        int line = lineCoverage[j];
                        if (line != -1) {
                            numLines++;
                        }
                        if (line > 0) {
                            coveredLines++;
                        }

                    }
                    int requiredCoverage = 100;
                    Coverage coverage = method.getAnnotation(Coverage.class);
                    if (coverage != null) {
                        requiredCoverage = coverage.value();
                    }
                    final int actualCoverage = coveredLines * 100 / numLines;

                    if (actualCoverage < requiredCoverage) {

                        TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/web/templates/coveragefailed.vm");
                        template.setAttribute("className", instrumentedClassUndertest.getName());
                        template.setAttribute("coveragePercent", actualCoverage);
                        template.setAttribute("requiredCoverage", requiredCoverage);
                        template.setAttribute("coveredLines", coveredLines);
                        template.setAttribute("totalLines", numLines);
                        template.setAttribute("lines", createLines(compiler.getSource(req), lineCoverage));
                        template.render(req, resp);
                        return false;
                    }
                }

            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private List<CodeLine> createLines(URL url, int[] coverage) {
        try {
            List<CodeLine> lines = new ArrayList<CodeLine>();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "iso-8859-1"));
            String line = null;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                lines.add(new CodeLine(lineNum, lineNum >= coverage.length ? -1 : coverage[lineNum], line));
            }
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, byte[]> instrumentCoverage(Map<String, byte[]> classes, Class registryClass) {
        Map<String, byte[]> instrumented = new HashMap<String, byte[]>();


        for (String name : classes.keySet()) {
            ClassReader reader = new ClassReader(classes.get(name));

            MaxLineNumberClassVisitor max = new MaxLineNumberClassVisitor();

            reader.accept(max, ClassReader.EXPAND_FRAMES);

            int classRef;
            try {
                final Method registerClass = registryClass.getMethod("registerClass", String.class, BitSet.class, new String[0].getClass());
                final Object ref = registerClass.invoke(null, name, max.getInstructionLines(), max.getMethodNames());
                classRef = ((Integer) ref).intValue();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            reader.accept(new InstrumentingClassVisitor(classRef, writer), ClassReader.EXPAND_FRAMES);

            instrumented.put(name, writer.toByteArray());
        }
        return instrumented;
    }
}
