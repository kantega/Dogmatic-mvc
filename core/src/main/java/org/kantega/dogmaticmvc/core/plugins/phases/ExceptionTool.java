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

/**
 */
public class ExceptionTool {
    private final Class testClass;
    private final Class clazz;

    public ExceptionTool(Class clazz, Class testClass) {
        this.clazz = clazz;
        this.testClass = testClass;
    }
    public String getFirstSourceMention(Throwable e) {
        for (int i = 0; i < e.getStackTrace().length; i++) {
            StackTraceElement ste = e.getStackTrace()[i];
            if(ste.getClassName().startsWith(clazz.getName()) ||ste.getClassName().startsWith(testClass.getName())) {
                return ste.getFileName() +":" + ste.getLineNumber();
            }
        }
        return null;
    }
}
