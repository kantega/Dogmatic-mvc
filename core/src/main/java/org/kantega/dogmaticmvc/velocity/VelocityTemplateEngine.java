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

package org.kantega.dogmaticmvc.velocity;

import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class VelocityTemplateEngine implements TemplateEngine {

    private final VelocityEngine engine;

    public VelocityTemplateEngine(VelocityEngine engine) {
        this.engine = engine;
    }

    @Override
    public Template createTemplate(String name) {
        return new VelocityTemplate(name);
    }

    private class VelocityTemplate implements Template {
        private String template;

        private Map<String, Object> context = new HashMap<String, Object>();

        public VelocityTemplate(String name) {
            this.template = name;
        }

        @Override
        public Template setAttribute(String name, Object value) {
            context.put(name, value);
            return this;
        }

        @Override
        public void render(HttpServletRequest request, HttpServletResponse response) {
            try {
                response.setContentType("text/html;charset=utf-8");
                VelocityContext vc = new VelocityContext(this.context);
                vc.put("root", request.getContextPath());
                engine.mergeTemplate(template, "iso-8859-1", vc, response.getWriter());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
