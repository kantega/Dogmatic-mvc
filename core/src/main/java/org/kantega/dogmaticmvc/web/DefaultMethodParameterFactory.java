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

import org.kantega.dogmaticmvc.RequestParam;
import org.kantega.dogmaticmvc.SessionAttr;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 */
public class DefaultMethodParameterFactory implements MethodParameterFactory {

    private ApplicationContext applicationContext;
    private final ServletContext servletContext;

    public DefaultMethodParameterFactory(ApplicationContext applicationContext, ServletContext servletContext) {
        this.applicationContext= applicationContext;
        this.servletContext = servletContext;
    }


    @Override
    public Object[] getMethodParameters(Method method, HttpServletRequest req, HttpServletResponse resp, Map<String, Object> model)  {
        Object[] params = new Object[method.getParameterTypes().length];


        try {
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                Class parameterType = method.getParameterTypes()[i];
                RequestParam requestParam;
                SessionAttr sessionAttr;
                if (parameterType == ServletContext.class) {
                    params[i] = servletContext;
                } else if (parameterType == HttpServletRequest.class) {
                    params[i] = req;
                } else if (parameterType == HttpSession.class) {
                    params[i] = req.getSession(true);
                } else if (parameterType == HttpServletResponse.class) {
                    params[i] = resp;
                } else if (parameterType == OutputStream.class) {
                    params[i] = resp.getOutputStream();
                } else if (parameterType == PrintWriter.class) {
                    params[i] = resp.getWriter();
                } else if (parameterType == ServletOutputStream.class) {
                    params[i] = resp.getOutputStream();
                } else if (parameterType == ApplicationContext.class && applicationContext != null) {
                    params[i] = applicationContext;
                } else if (parameterType == Map.class) {
                    params[i] = model;
                } else if ((requestParam = getParamAnnotation(method, i, RequestParam.class)) != null) {
                    String name = requestParam.value();
                    String value = req.getParameter(name);
                    if (value == null) {
                        if (requestParam.required()) {
                            throw new IllegalArgumentException("Missing required parameter '" + name + "'");
                        }
                        params[i] = null;
                    } else if (parameterType == String.class) {
                        params[i] = value;
                    } else if (parameterType == int.class || parameterType == Integer.class) {
                        params[i] = Integer.parseInt(value);
                    } else if (parameterType == long.class || parameterType == Long.class) {
                        params[i] = Long.parseLong(value);
                    } else if (parameterType == boolean.class || parameterType == Boolean.class) {
                        params[i] = Boolean.parseBoolean(value);
                    } else {
                        throw new RuntimeException("Unknown parameter type " + parameterType);
                    }
                } else if ((sessionAttr = getParamAnnotation(method, i, SessionAttr.class)) != null) {
                    String name = sessionAttr.value();
                    Object value = req.getSession(true).getAttribute(name);
                    if (value == null) {
                        if (sessionAttr.required()) {
                            throw new IllegalArgumentException("Missing required session attribute '" + name + "'");
                        }
                        params[i] = null;
                    } else if (parameterType.isAssignableFrom(value.getClass())) {
                        params[i] = value;
                    } else {
                        throw new ServletException("Session value of type " + value.getClass() + "is not assignalble to parameter " + name + " of type " + parameterType);
                    }
                } else if (applicationContext != null && applicationContext.getBeansOfType(parameterType).size() > 1) {
                    throw new ServletException("Spring context has multiple beans of type " + parameterType.getName());
                } else if (applicationContext != null && applicationContext.getBeansOfType(parameterType).size() == 1) {
                    params[i] = applicationContext.getBeansOfType(parameterType).values().iterator().next();
                } else if (applicationContext!= null && applicationContext.getBeansOfType(parameterType).size() == 0) {
                    throw new ServletException("Unknown parameter type " + parameterType.getName());
                } else {
                    throw new ServletException("Unknown parameter type " + parameterType.getName());
                }


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return params;
    }

    private <T extends Annotation> T getParamAnnotation(Method method, int i, Class<T> annotation) {
        for (Annotation a : method.getParameterAnnotations()[i]) {
            if (annotation.isAssignableFrom(a.getClass())) {
                return (T) a;
            }
        }
        return null;
    }

    protected void setApplicationContext(ApplicationContext context) {
        this.applicationContext = context;
    }
}
