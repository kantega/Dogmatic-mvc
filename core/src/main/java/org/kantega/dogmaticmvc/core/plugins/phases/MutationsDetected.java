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

import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.VerificationPhase;
import org.kantega.dogmaticmvc.mutation.MutationClassVisitor;
import org.kantega.dogmaticmvc.testrun.RequestMethodRunner;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.kantega.dogmaticmvc.web.DogmaticMVCHandler.findSameMethod;
import static org.kantega.dogmaticmvc.web.DogmaticMVCHandler.getTestClass;

/**
 *
 */
public class MutationsDetected implements VerificationPhase {

    private final TemplateEngine templateEngine;


    public MutationsDetected(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {
        {
            // Mutation testing

            Map<String, byte[]> mutatedClasses = new HashMap<String, byte[]>();

            // Copy all class bytes just to be safe

            Map<String, byte[]> compiledClasses = new HashMap<String, byte[]>(compiledBytes);
            for (String name : compiledClasses.keySet()) {
                byte[] bytes = compiledClasses.get(name);
                byte[] bytesCopy = new byte[bytes.length];
                System.arraycopy(bytes, 0, bytesCopy, 0, bytes.length);
                mutatedClasses.put(name, bytesCopy);
            }

            int mutations = instrumentMutation(method.getDeclaringClass().getName(), mutatedClasses);

            if (mutations > 0) {


                final BytesMapClassLoader mutatingLoader = new BytesMapClassLoader(getClass().getClassLoader(), mutatedClasses);

                try {
                    Class mutatedClass = mutatingLoader.loadClass(method.getDeclaringClass().getName());

                    Class testClassForMutation = getTestClass(findSameMethod(method, mutatedClass));

                    final RequestMethodRunner runner = new RequestMethodRunner(testClassForMutation);
                    final RunNotifier notifier = new RunNotifier();
                    final Result result = new Result();
                    notifier.addListener(result.createListener());
                    runner.run(notifier);
                    if (result.wasSuccessful()) {
                        TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/web/templates/mutationfailed.vm");
                        template.setAttribute("className", mutatedClass.getName());
                        template.render(req, resp);
                        return false;
                    }
                } catch (InitializationError initializationError) {
                    throw new RuntimeException(initializationError);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return true;
    }

    private int instrumentMutation(String className, Map<String, byte[]> classes) {


        ClassReader reader = new ClassReader(classes.get(className));

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        MutationClassVisitor mcv = new MutationClassVisitor(writer);
        reader.accept(mcv, ClassReader.EXPAND_FRAMES);

        classes.put(className, writer.toByteArray());

        return mcv.getMutationCounts();
    }

}
