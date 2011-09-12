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

package org.kantega.dogmaticmvc.groovy;

import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.tools.GroovyClass;
import org.kantega.dogmaticmvc.api.ScriptCompiler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class GroovyScriptCompiler implements ScriptCompiler {

    private final GroovyScriptEngine engine;
    private final ServletContext servletContext;

    public GroovyScriptCompiler(ServletContext servletContext) {
        this.servletContext = servletContext;
        List<URL> roots = new ArrayList<URL>();
        final URL resource;
        try {
            resource = servletContext.getResource("/WEB-INF/dogmatic/");

            if (resource != null) {
                roots.add(resource);

                File dir = new File(resource.getFile());

                if (dir.isDirectory()) {

                    // Ugly hack to force classes to be loaded from scripts if they are
                    // also on the normal classpath which happens with an IDE
                    for (File f : dir.listFiles()) {
                        if (f.isFile() && f.getName().endsWith(".groovy")) {
                            f.setLastModified(System.currentTimeMillis());
                        }
                    }
                }
            }

            engine = new GroovyScriptEngine(roots.toArray(new URL[roots.size()]), getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean canHandle(HttpServletRequest request) {
        final String groovyPath = getGroovyPath(request);


        try {
            engine.getResourceConnection(groovyPath);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    @Override
    public Map<String, byte[]> getCompiledClassBytes(HttpServletRequest req) {
        GroovyScriptEngine engine;
        final List<CompilationUnit> units = new ArrayList<CompilationUnit>();
        try {
            List<URL> roots = new ArrayList<URL>();
            roots.add(servletContext.getResource("/WEB-INF/dogmatic/"));
            engine = new GroovyScriptEngine(roots.toArray(new URL[roots.size()]), getClass().getClassLoader()) {
                @Override
                protected void customizeCompilationUnit(CompilationUnit cu) {

                    units.add(cu);
                }
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try {
            engine.loadScriptByName(getGroovyPath(req));
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        Map<String, byte[]> compiledClasses = new HashMap<String, byte[]>();

        for (GroovyClass clazz : (List<GroovyClass>) units.get(0).getClasses()) {
            compiledClasses.put(clazz.getName(), clazz.getBytes());
        }

        return compiledClasses;
    }

    @Override
    public URL getSource(HttpServletRequest req) {
        try {
            return engine.getResourceConnection(getGroovyPath(req)).getURL();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class compile(HttpServletRequest request) {
        String groovyPath = getGroovyPath(request);
        try {
            return engine.loadScriptByName(groovyPath);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private String getGroovyPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI().substring("/".length());
        if (requestURI.equals("") || requestURI.endsWith("/")) {
            requestURI += "Index";
        }
        final String groovyPath = requestURI + ".groovy";
        return groovyPath;
    }

}
