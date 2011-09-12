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

package org.kantega.dogmaticmvc.instrumentation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 */
public class MaxLineNumberClassVisitor extends EmptyVisitor {
    private BitSet instructionLines = new BitSet();

    private List<String> methodNames = new ArrayList<String>();
    private int access;
    private String source;

    @Override
    public void visitSource(String source, String debug) {
        this.source = source;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, signature, interfaces);
        this.access = access;

    }

    public MethodVisitor visitMethod(int access, final String name, String s1, String s2, String[] strings) {

        methodNames.add(name);

        return new EmptyVisitor() {

            @Override
            public void visitLineNumber(int lineNumber, Label label) {
                if(lineNumber < 0) {
                    throw new IllegalStateException("WTF: " +name + " got line number " + lineNumber);
                }
                instructionLines.set(lineNumber);
            }

        };

    }



    public BitSet getInstructionLines() {
        return instructionLines;
    }

    public String[] getMethodNames() {
        return methodNames.toArray(new String[methodNames.size()]);
    }

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    public String getSource() {
        return source;
    }
}

