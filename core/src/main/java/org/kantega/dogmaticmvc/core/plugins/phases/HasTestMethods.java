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

import org.kantega.dogmaticmvc.TestWith;
import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.api.VerificationPhase;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 */
public class HasTestMethods implements VerificationPhase {
    private final TemplateEngine templateEngine;

    public HasTestMethods(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {
        TestWith annotation = method.getAnnotation(TestWith.class);
        if (annotation == null || !hasTestMethods(annotation.value())) {
            TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/web/templates/notestsfound.vm");
            template.setAttribute("className", method.getDeclaringClass().getName());
            template.setAttribute("method", method.getName());
            template.render(req, resp);
            return false;
        } else {
            return true;
        }
    }

    private boolean hasTestMethods(Class groovyClass) {
        for (Method method : groovyClass.getMethods()) {
            if (method.getAnnotation(Test.class) != null) {
                return true;
            }
        }
        return false;
    }
}
