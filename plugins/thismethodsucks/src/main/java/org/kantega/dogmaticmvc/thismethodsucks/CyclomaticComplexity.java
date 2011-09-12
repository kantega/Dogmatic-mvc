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

package org.kantega.dogmaticmvc.thismethodsucks;

import org.kantega.dogmaticmvc.api.ScriptCompiler;
import org.kantega.dogmaticmvc.api.TemplateEngine;
import org.kantega.dogmaticmvc.api.VerificationPhase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 */
public class CyclomaticComplexity implements VerificationPhase {
    private final TemplateEngine templateEngine;

    public CyclomaticComplexity(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean verify(Method method, ScriptCompiler compiler, Map<String, byte[]> compiledBytes, HttpServletRequest req, HttpServletResponse resp) {
        byte[] bytes = compiledBytes.get(method.getDeclaringClass().getName());

        ClassReader reader = new ClassReader(bytes);


        CyclomaticComplexityVisitor complexityVisitor = new CyclomaticComplexityVisitor(method);
        reader.accept(complexityVisitor, ClassReader.EXPAND_FRAMES);


        ThisMethodSucks sucks = method.getAnnotation(ThisMethodSucks.class);

        int limit = sucks != null ? sucks.value() : 8;

        if(complexityVisitor.getComplexity() > limit) {
            TemplateEngine.Template template = templateEngine.createTemplate("org/kantega/dogmaticmvc/thismethodsucks/methodsucks.vm");
            template.setAttribute("complexity", complexityVisitor.getComplexity());
            template.render(req, resp);
            return false;
        }
        return true;

    }

    class CyclomaticComplexityVisitor extends EmptyVisitor {
        private int complexity = 1;
        private final Method method;

        public CyclomaticComplexityVisitor(Method method) {
            this.method = method;
        }

        public int getComplexity() {
            return complexity;
        }

        @Override
        public MethodVisitor visitMethod(int i, String name, String s1, String s2, String[] strings) {
            MethodVisitor superVisitor = super.visitMethod(i, name, s1, s2, strings);

            if(name.equals(method.getName())) {
                return new CyclomaticComplexityMethodVisitor();
            }
            return superVisitor;
        }

        private class CyclomaticComplexityMethodVisitor extends EmptyVisitor {
            @Override
            public void visitJumpInsn(int i, Label label) {
                complexity++;
            }
        }
    }
}
