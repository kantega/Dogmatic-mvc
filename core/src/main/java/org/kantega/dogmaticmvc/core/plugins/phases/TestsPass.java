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

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.kantega.dogmaticmvc.TestWith;
import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.api.VerificationPhase;
import org.kantega.dogmaticmvc.testrun.RequestMethodRunListener;
import org.kantega.dogmaticmvc.testrun.RequestMethodRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 */
public class TestsPass implements VerificationPhase {
    private final TemplateEngine templateEngine;

    public TestsPass(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {
        try {

            Class testClass = method.getAnnotation(TestWith.class).value();
            final RequestMethodRunner runner = new RequestMethodRunner(testClass);
            final RunNotifier notifier = new RunNotifier();
            final RequestMethodRunListener listener = new RequestMethodRunListener();
            notifier.addListener(listener);
            runner.run(notifier);
            if (listener.getTestFailures().size() > 0) {
                TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/web/templates/testfailed.vm");

                template.setAttribute("failures", listener.getTestFailures());
                template.setAttribute("exceptionTool", new ExceptionTool(method.getDeclaringClass(), testClass));
                template.setAttribute("testClass", testClass);
                template.render(req, resp);
                return false;
            }
        } catch (InitializationError e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
