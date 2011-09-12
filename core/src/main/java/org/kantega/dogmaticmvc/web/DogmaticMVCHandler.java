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

package org.kantega.dogmaticmvc.web;

import org.kantega.dogmaticmvc.RequestHandler;
import org.kantega.dogmaticmvc.TestWith;
import org.kantega.dogmaticmvc.api.*;
import org.kantega.jexmec.PluginManager;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 *
 */
public class DogmaticMVCHandler implements Handler {
    private MethodParameterFactory parameterFactory;
    private final TemplateEngine templateEngine;
    private final ServletContext servletContext;
    private List<ScriptCompiler> scriptCompilers;
    private final PluginManager<DogmaticPlugin> pluginManager;

    public DogmaticMVCHandler(ServletContext servletContext, PluginManager<DogmaticPlugin> pluginManager, TemplateEngine templateEngine) throws ServletException {
        this.servletContext = servletContext;
        this.pluginManager = pluginManager;
        this.templateEngine = templateEngine;
        parameterFactory = new DefaultMethodParameterFactory(WebApplicationContextUtils.getWebApplicationContext(servletContext), servletContext);
        scriptCompilers = new ArrayList<ScriptCompiler>();

        for(DogmaticPlugin plugin : pluginManager.getPlugins()) {
            scriptCompilers.addAll(plugin.getScriptCompilers());
        }

    }


    @Override
    public boolean canHandle(HttpServletRequest request) {
        for(ScriptCompiler compiler : scriptCompilers) {
            if(compiler.canHandle(request)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse resp) {


        final ScriptCompiler compiler = getScriptCompiler(request);
        final Class handlerClass = compiler.compile(request);
        Map<String, byte[]> compiledClassBytes = compiler.getCompiledClassBytes(request);

        Method handlerMethod = findRequestHandlerMethod(handlerClass);

        if (handlerMethod == null) {
            TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/web/templates/norequestmethod.vm");
            template.setAttribute("className", handlerClass.getName());
            template.render(request, resp);
            return;

        }


        try {
            if (!runVerificationPhases(handlerMethod, request, resp, compiler, compiledClassBytes)) {
                return;
            }

            Map<String, Object> model = new HashMap<String, Object>();

            Object[] params = parameterFactory.getMethodParameters(handlerMethod, request, resp, model);

            final Object result = handlerMethod.invoke(handlerClass.newInstance(), params);

            if (!resp.isCommitted() && result instanceof String) {
                for (Map.Entry<String, Object> e : model.entrySet()) {
                    request.setAttribute(e.getKey(), e.getValue());
                }
                servletContext.getRequestDispatcher(String.class.cast(result)).forward(request, wrapResponse(request, resp));
            }
            return;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private ServletResponse wrapResponse(HttpServletRequest request, HttpServletResponse resp) {
        HttpServletResponse wrapped = resp;
        for(DogmaticPlugin plugin : pluginManager.getPlugins()) {
            for(ResponseWrapper wrapper : plugin.getResponseWrappers()) {
                wrapped = wrapper.wrap(request, wrapped);
            }
        }
        return wrapped;
    }

    private ScriptCompiler getScriptCompiler(HttpServletRequest request) {
        for(ScriptCompiler compiler : scriptCompilers) {
            if(compiler.canHandle(request)) {
                return compiler;
            }
        }
        throw new IllegalStateException("No ScriptCompiler can handle this request");
    }

    private Method findRequestHandlerMethod(Class clazz) {
        for (Method method : clazz.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof RequestHandler) {
                    return method;
                }
            }
        }
        return null;
    }



    private boolean runVerificationPhases(final Method handlerMethod, HttpServletRequest req, HttpServletResponse resp, ScriptCompiler compiler, Map<String, byte[]> compiledClassBytes) throws ServletException {

        List<VerificationPhase> phases = new ArrayList<VerificationPhase>();

        for(DogmaticPlugin plugin : pluginManager.getPlugins()) {
            phases.addAll(plugin.getVerificationPhases());
        }
        for(VerificationPhase phase : phases) {
            if(!phase.verify(handlerMethod, compiler, compiledClassBytes, req, resp)) {
                return false;
            }
        }

        return true;
    }





    public static Class getTestClass(Method method) {
        final TestWith testWith = method.getAnnotation(TestWith.class);
        return testWith != null ? testWith.value() : method.getDeclaringClass();
    }



    public static Method findSameMethod(Method method, Class classToTest) {
        for (Method m : classToTest.getMethods()) {
            if (m.getName().equals(method.getName()) && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) {
                method = m;
                break;
            }
        }
        return method;
    }



    public static byte[] toBytes(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        out.flush();
        return out.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        while (true) {
            int count = in.read(buf, 0, buf.length);
            if (count == -1) break;
            if (count == 0) {
                Thread.yield();
                continue;
            }
            out.write(buf, 0, count);
        }
    }
}
