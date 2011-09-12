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

import org.kantega.dogmaticmvc.api.Handler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

/**
 *
 */
public class StaticResourceHandler implements Handler {
    private final ServletContext servletContext;

    public StaticResourceHandler(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public boolean canHandle(HttpServletRequest request) {
        String sp = request.getServletPath();
        return !sp.endsWith("/") && getResource(sp) != null;

    }



    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        URL resource = getResource(request.getServletPath());

        response.setContentType(servletContext.getMimeType(request.getServletPath()));
        try {
            DogmaticMVCHandler.copy(resource.openStream(), response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL getResource(String servletPath) {
        return getClass().getClassLoader().getResource("org/kantega/dogmaticmvc/static" + servletPath);
    }
}
