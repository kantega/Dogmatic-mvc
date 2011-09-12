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

package org.kantega.dogmaticmvc.mutation;

import org.kantega.dogmaticmvc.RequestHandler;
import org.objectweb.asm.*;

/**
 */
public class MutationMethodVisitor extends MethodAdapter {
    int jumps = 0;
    private int currentLine;

    private boolean shouldMutate = false;
    private MutationClassVisitor cv;

    public MutationMethodVisitor(MethodVisitor methodVisitor, MutationClassVisitor mutationClassVisitor) {
        super(methodVisitor);
        this.cv = mutationClassVisitor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, boolean b) {
        if(Type.getType(name).getClassName().equals(RequestHandler.class.getName())) {
            shouldMutate = true;
        }   
        return super.visitAnnotation(name, b);
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        super.visitLineNumber(i, label);
        currentLine = i;
    }

@Override
public void visitJumpInsn(int i, Label label) {
    if(jumps++ == 0) {
        System.out.println("Mutating jump instruction at line " + currentLine);
        int jumpInsn = getMutatedJumpInsn(i);
        super.visitJumpInsn(jumpInsn, label);
        if(jumpInsn != i) {
            cv.mutationCounts++;
        }
    } else {
        super.visitJumpInsn(i, label);
    }

}

private int getMutatedJumpInsn(int i) {
    switch(i) {
        case Opcodes.IFEQ:
            return Opcodes.IFNE;
        case Opcodes.IFNE:
            return Opcodes.IFEQ;
        case Opcodes.IFLT:
            return Opcodes.IFGE;
        case Opcodes.IFGE:
            return Opcodes.IFLT;
        case Opcodes.IFGT:
            return Opcodes.IFLE;
        case Opcodes.IFLE:
            return Opcodes.IFGT;
        case Opcodes.IF_ICMPEQ:
            return Opcodes.IF_ICMPNE;
        case Opcodes.IF_ICMPNE:
            return Opcodes.IF_ICMPEQ;
        case Opcodes.IF_ICMPLT:
            return Opcodes.IF_ICMPGE;
        case Opcodes.IF_ICMPGE:
            return Opcodes.IF_ICMPLT;
        case Opcodes.IF_ICMPGT:
            return Opcodes.IF_ICMPLE;
        case Opcodes.IF_ICMPLE:
            return Opcodes.IF_ICMPGT;
        case Opcodes.IF_ACMPEQ:
            return Opcodes.IF_ACMPNE;
        case Opcodes.IF_ACMPNE:
            return Opcodes.IF_ACMPEQ;
        case Opcodes.IFNULL:
            return Opcodes.IFNONNULL;
        case Opcodes.IFNONNULL:
            return Opcodes.IFNULL;

        default:
            return i;
    }
}

    static class AnnotationAdapter implements AnnotationVisitor {

        private AnnotationVisitor visitor;

        AnnotationAdapter(AnnotationVisitor visitor) {
            this.visitor = visitor;
        }

        public void visit(String s, Object o) {
            visitor.visit(s, o);
        }

        public void visitEnum(String s, String s1, String s2) {
            visitor.visitEnum(s, s1, s2);
        }

        public AnnotationVisitor visitAnnotation(String s, String s1) {
            return visitor.visitAnnotation(s, s1);
        }

        public AnnotationVisitor visitArray(String s) {
            return visitor.visitArray(s);
        }

        public void visitEnd() {
            visitor.visitEnd();
        }
    }
}

