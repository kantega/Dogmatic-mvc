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

import org.kantega.dogmaticmvc.api.DogmaticPluginBase;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.api.VerificationPhase;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class CoreVerificationPhasesPlugin extends DogmaticPluginBase {

    private List<VerificationPhase> phases;

    public CoreVerificationPhasesPlugin(ServletContext servletContext, TemplateEngine templateEngine) {
        phases = Arrays.asList(new HasTestMethods(templateEngine),
                new TestsPass(templateEngine),
                new SufficientTestCoverage(servletContext, templateEngine),
                new MutationsDetected(templateEngine));


    }

    @Override
    public List<VerificationPhase> getVerificationPhases() {
        return phases;
    }
}
