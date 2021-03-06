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

package org.kantega.dogmaticmvc.api;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 *
 */
public class DogmaticPluginBase implements  DogmaticPlugin {
    public List<ScriptCompiler> getScriptCompilers() {return emptyList();}
    public List<VerificationPhase> getVerificationPhases() {return emptyList();}
    public List<Handler> getHandlers() {return emptyList();}
    public List<ResponseWrapper> getResponseWrappers() {return emptyList();}
}
