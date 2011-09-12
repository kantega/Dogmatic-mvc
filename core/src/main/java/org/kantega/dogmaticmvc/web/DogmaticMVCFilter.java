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

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.kantega.dogmaticmvc.api.DogmaticPlugin;
import org.kantega.dogmaticmvc.api.Handler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.velocity.DispatchDirective;
import org.kantega.dogmaticmvc.velocity.HtmlEscapeDirective;
import org.kantega.dogmaticmvc.velocity.SectionDirective;
import org.kantega.dogmaticmvc.velocity.VelocityTemplateEngine;
import org.kantega.jexmec.PluginManager;
import org.kantega.jexmec.ServiceKey;
import org.kantega.jexmec.ctor.ConstructorInjectionPluginLoader;
import org.kantega.jexmec.manager.DefaultPluginManager;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DogmaticMVCFilter implements Filter {


    private static ThreadLocal<HttpServletRequest> request = new ThreadLocal<HttpServletRequest>();
    private static ThreadLocal<HttpServletResponse> response = new ThreadLocal<HttpServletResponse>();

    private List<Handler> handlers;
    private PluginManager<DogmaticPlugin> pluginManager;


    public void init(FilterConfig config) throws ServletException {
        TemplateEngine templateEngine = initVelocityTemplateEngine();
        pluginManager = createPluginManager(config.getServletContext(), templateEngine);

        handlers = new ArrayList<Handler>();
        handlers.add(new StaticResourceHandler(config.getServletContext()));
        handlers.add(new DogmaticMVCHandler(config.getServletContext(), pluginManager, templateEngine));
        for(DogmaticPlugin plugin : pluginManager.getPlugins()) {
            handlers.addAll(plugin.getHandlers());
        }
    }

    private PluginManager<DogmaticPlugin> createPluginManager(ServletContext servletContext, TemplateEngine templateEngine) {
        DefaultPluginManager<DogmaticPlugin> manager = new DefaultPluginManager<DogmaticPlugin>(DogmaticPlugin.class);
        manager.addPluginClassLoader(getClass().getClassLoader());
        manager.addPluginLoader(new ConstructorInjectionPluginLoader<DogmaticPlugin>());
        manager.addService(ServiceKey.by(ServletContext.class), servletContext);
        manager.addService(ServiceKey.by(TemplateEngine.class), templateEngine);
        manager.start();
        return manager;

    }

    private TemplateEngine initVelocityTemplateEngine() throws ServletException {

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class", org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader.class.getName());
        velocityEngine.addProperty("userdirective", SectionDirective.class.getName());
        velocityEngine.addProperty("userdirective", DispatchDirective.class.getName());
        velocityEngine.addProperty("userdirective", HtmlEscapeDirective.class.getName());
        velocityEngine.setProperty("eventhandler.include.class", IncludeRelativePath.class.getName());
        try {
            velocityEngine.init();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        return new VelocityTemplateEngine(velocityEngine);
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        DogmaticMVCFilter.request.set(req);


        try {
            req.setCharacterEncoding("utf-8");
            req.getParameterMap();

            for(Handler handler : handlers) {
                if(handler.canHandle(req)) {
                    handler.handle(req, resp);
                    return;
                }
            }
            chain.doFilter(req, resp);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }finally {
            DogmaticMVCFilter.request.remove();
        }
    }
    
    public static HttpServletRequest getRequest() {
        return request.get();
    }

    public static HttpServletResponse getResponse() {
        return response.get();
    }

}

