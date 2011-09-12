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

import org.kantega.dogmaticmvc.api.*;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class AgilePlugin extends DogmaticPluginBase {

    private final List<ResponseWrapper> wrappers;

    private final List<VerificationPhase> phases;

    private final List<Handler> handlers;

    public AgilePlugin(TemplateEngine templateEngine, ServletContext servletContext) {
        wrappers = Collections.<ResponseWrapper>singletonList(new AgileResponseWrapper());
        phases = Collections.<VerificationPhase>singletonList(new AgileVerificationPhase(templateEngine, servletContext));
        handlers = Collections.<Handler>singletonList(new ContinueHandler(servletContext));
    }



    @Override
    public List<ResponseWrapper> getResponseWrappers() {
        return wrappers;
    }

    @Override
    public List<VerificationPhase> getVerificationPhases() {
        return phases;
    }

    @Override
    public List<Handler> getHandlers() {
        return handlers;
    }
}
