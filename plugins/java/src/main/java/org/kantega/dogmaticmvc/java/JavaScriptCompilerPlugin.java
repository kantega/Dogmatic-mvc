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

package org.kantega.dogmaticmvc.java;

import org.kantega.dogmaticmvc.api.DogmaticPluginBase;
import org.kantega.dogmaticmvc.api.ScriptCompiler;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class JavaScriptCompilerPlugin extends DogmaticPluginBase {

    private List<ScriptCompiler> compilers;

    public JavaScriptCompilerPlugin(ServletContext servletContext) {
        this.compilers = Collections.<ScriptCompiler>singletonList(new JavaScriptCompiler(servletContext));
    }

    @Override
    public List<ScriptCompiler> getScriptCompilers() {
        return compilers;
    }
}
