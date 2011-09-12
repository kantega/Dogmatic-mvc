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

package org.kantega.dogmaticmvc.agile;

import org.kantega.dogmaticmvc.api.Handler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class ContinueHandler implements Handler {

    private final ServletContext servletContext;

    public ContinueHandler(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public boolean canHandle(HttpServletRequest request) {
        return request.getMethod().equals("POST") && request.getServletPath().equals("/agile/continue");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {

        String servletPath = request.getParameter("servletPath");
        String source = servletContext.getRealPath("/agile" + servletPath + ".agile");
        File agile = new File(source);
        try {
            agile.getParentFile().mkdirs();
            agile.createNewFile();
            response.sendRedirect(servletPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
