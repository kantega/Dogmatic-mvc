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

package org.kantega.dogmaticmvc.java;

import org.apache.commons.io.IOUtils;
import org.kantega.dogmaticmvc.api.ScriptCompiler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.tools.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 */
public class JavaScriptCompiler implements ScriptCompiler {
    private final ServletContext servletContext;

    private Map<String, JavaClassObject> classes = new HashMap<String, JavaClassObject>();
    
    public JavaScriptCompiler(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public boolean canHandle(HttpServletRequest request) {
        return getJavaFileAsResource(request) != null;
    }

    private URL getJavaFileAsResource(HttpServletRequest request) {
        try {
            return servletContext.getResource("/WEB-INF/dogmatic" + request.getServletPath() + ".java");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class compile(HttpServletRequest request) {

        String className = request.getServletPath().substring(1).replace('/', '.');

        List<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
        for(String path : (Set<String>) servletContext.getResourcePaths("/WEB-INF/dogmatic/")) {
            if(path.endsWith("java")) {
                try {
                    String classNAme = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
                    compilationUnits.add(new JavaSourceFromURL(classNAme, servletContext.getResource(path)));
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(compiler.getStandardFileManager(null, null, null), new ClassLoaderImpl(getClass().getClassLoader()));



        String cp = "";
        for(ClassLoader cl : Arrays.asList(getClass().getClassLoader(), getClass().getClassLoader().getParent())) {
            if(cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader) cl;

                for(URL url : ucl.getURLs()) {
                    if(cp.length() > 0) {
                        cp +=File.pathSeparator;
                    }
                    cp += url.getFile();
                }
            }
        }
        List<String> options = new ArrayList(Arrays.asList("-classpath", cp));

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);


        boolean success = task.call();
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        
        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            w.println(diagnostic.getCode());
            w.println(diagnostic.getKind());
            w.println(diagnostic.getPosition());
            w.println(diagnostic.getStartPosition());
            w.println(diagnostic.getEndPosition());
            w.println(diagnostic.getSource());
            w.println(diagnostic.getMessage(null));

        }
        System.out.println("Success: " + success);

        if (success) {
            try {
                return fileManager.getClassLoader(null).loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Compilation failed: " + sw);
        }
    }

    @Override
    public Map<String, byte[]> getCompiledClassBytes(HttpServletRequest req) {
        Map<String, byte[]> bytesMap = new HashMap<String, byte[]>();
        for(Map.Entry<String, JavaClassObject> entry : classes.entrySet()) {
            bytesMap.put(entry.getKey(), entry.getValue().getBytes());
        }
        return bytesMap;
    }

    @Override
    public URL getSource(HttpServletRequest req) {
        return getJavaFileAsResource(req);
    }

    private class ClassLoaderImpl extends ClassLoader {

        private ClassLoaderImpl(ClassLoader classLoader) {
            super(classLoader);
        }

        public void put(String className, JavaClassObject classObject) {
            classes.put(className, classObject);
        }

        @Override
        public Class<?> loadClass(String s) throws ClassNotFoundException {
            return super.loadClass(s);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        protected Class<?> loadClass(String s, boolean b) throws ClassNotFoundException {
            return super.loadClass(s, b);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if(classes.containsKey(name)) {
                byte[] bytes = classes.get(name).getBytes();
                return super.defineClass(name, bytes, 0, bytes.length);
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    }
    private class InMemoryJavaFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<JavaFileManager> {
        private ClassLoaderImpl classLoader;

        public InMemoryJavaFileManager(JavaFileManager fileManager, ClassLoaderImpl classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject fileObject) throws IOException {
            JavaClassObject classObject = new JavaClassObject(className, kind);
            classLoader.put(className, classObject);
            return classObject;
        }


        @Override
        public ClassLoader getClassLoader(Location location) {
            return classLoader;
        }

        
        public Iterable<JavaFileObject> list(Location location, java.lang.String packageName, Set<JavaFileObject.Kind> kinds, boolean recursive) throws IOException {
            Iterable<JavaFileObject> files = super.list(location, packageName, kinds, recursive);
            return files;
        }
    }

    class JavaSourceFromURL extends SimpleJavaFileObject {
        private final URL code;
        private final String name;

        JavaSourceFromURL (String name, URL code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
            this.name = name;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return IOUtils.toString(code.openStream(), "utf-8");
        }

        @Override
        public boolean isNameCompatible(String s, Kind kind) {
            return super.isNameCompatible(s, kind);
        }

        @Override
        public URI toUri() {
            try {
                return code.toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getName() {
            return super.getName();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    class JavaClassObject extends SimpleJavaFileObject {

        private ByteArrayOutputStream out = new ByteArrayOutputStream();

        JavaClassObject(String name, Kind kind) {
             super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return out;
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }
    }
}
