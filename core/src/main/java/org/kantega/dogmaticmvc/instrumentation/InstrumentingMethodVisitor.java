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
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 */
public class InstrumentingMethodVisitor extends MethodAdapter implements Opcodes {
    private final int classRef;
    private final int methodRef;

    private int lineCountLocalVariable;

    public InstrumentingMethodVisitor(int classRef, int methodRef, MethodVisitor methodVisitor) {
        super(methodVisitor);
        this.classRef = classRef;
        this.methodRef = methodRef;
    }

    @Override
    public void visitLineNumber(final int lineNumber, Label label) {
        {
            mv.visitLineNumber(lineNumber, label);

            //System.out.println("Instrumenting line " +lineNumber +" in " + LineVisitRegistry.classNames[classRef] +"/"       + LineVisitRegistry.methodNames[classRef][methodRef] +" to " + mv);

            // Line count
            {
                mv.visitVarInsn(ALOAD, lineCountLocalVariable);
                mv.visitIntInsn(lineNumber <= Byte.MAX_VALUE && lineNumber >= Byte.MIN_VALUE ? BIPUSH : SIPUSH, lineNumber);
                mv.visitInsn(DUP2);
                mv.visitInsn(IALOAD);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(IADD);
                mv.visitInsn(IASTORE);
            }
        }

    }


    public void setLineCountLocalVariable(int lineCountLocalVariable) {
        this.lineCountLocalVariable = lineCountLocalVariable;
    }

}

