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

import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.api.VerificationPhase;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 */
public class AgileVerificationPhase implements VerificationPhase {
    private final TemplateEngine templateEngine;
    private final ServletContext servletContext;

    public AgileVerificationPhase(TemplateEngine templateEngine, ServletContext servletContext) {
        this.templateEngine = templateEngine;
        this.servletContext = servletContext;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {
        boolean claimsToBeAgile = method.getAnnotation(Agile.class) != null;
        if(claimsToBeAgile) {

            File agileFile = new File(servletContext.getRealPath("/agile" + req.getServletPath()) + ".agile");
            if(!agileFile.exists()) {
                TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/agile/agilevalues.vm");
                template.setAttribute("servletPath", req.getServletPath());
                template.render(req, resp);
                return false;
            } else {
                req.setAttribute("isAgile", Boolean.TRUE);
            }

        }

        return true;
    }
}
