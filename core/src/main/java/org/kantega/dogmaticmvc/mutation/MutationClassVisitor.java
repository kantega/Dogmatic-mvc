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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 */
public class MutationClassVisitor extends ClassAdapter {
    protected int mutationCounts;

    public MutationClassVisitor(ClassVisitor classVisitor) {
        super(classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int i, String name, String s1, String s2, String[] strings) {
        final MethodVisitor superVisitor = super.visitMethod(i, name, s1, s2, strings);
        if(name.contains("<") || name.contains("$") || (i & Opcodes.ACC_SYNTHETIC) != 0) {
            return superVisitor;
        }
        return new MutationMethodVisitor(superVisitor, this);
    }

    public int getMutationCounts() {
        return mutationCounts;
    }
}
