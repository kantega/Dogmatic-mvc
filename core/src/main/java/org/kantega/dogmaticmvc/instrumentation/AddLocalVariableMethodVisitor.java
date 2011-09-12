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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 */
public class AddLocalVariableMethodVisitor extends LocalVariablesSorter implements Opcodes {
    private final int classRef;
    private int lineCountsLocalVariable;
    private InstrumentingMethodVisitor lineConverageVisitor;

    public AddLocalVariableMethodVisitor(int classRef, int access, String desc, MethodVisitor methodVisitor) {
        super(access, desc, methodVisitor);
        this.classRef = classRef;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, InstrumentingClassVisitor.LINEVISITREGISTRY, "lineCounts", "[[I");
        mv.visitIntInsn(classRef <= Byte.MAX_VALUE && classRef >= Byte.MIN_VALUE ? BIPUSH : SIPUSH, classRef);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, lineCountsLocalVariable = newLocal(Type.getObjectType("[[I")));

        lineConverageVisitor.setLineCountLocalVariable(lineCountsLocalVariable);
    }

    public int getLineCountsLocalVariable() {
        return lineCountsLocalVariable;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack +4, maxLocals);
    }

    public void setLineConverageVisitor(InstrumentingMethodVisitor lineConverageVisitor) {
        this.lineConverageVisitor = lineConverageVisitor;
    }
}
