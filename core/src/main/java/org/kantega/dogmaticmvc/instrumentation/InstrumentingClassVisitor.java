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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 */
public class InstrumentingClassVisitor extends ClassAdapter {
    private final ClassVisitor classVisitor;
    private final int classRef;

    private int methodCount = 0;

    public static final String LINEVISITREGISTRY = LineVisitorRegistry.class.getName().replace('.','/');

    public InstrumentingClassVisitor(int classRef, ClassVisitor classVisitor) {
        super(classVisitor);
        this.classVisitor = classVisitor;
        this.classRef = classRef;

    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        InstrumentingMethodVisitor lineCoverage = new InstrumentingMethodVisitor(classRef, methodCount++, classVisitor.visitMethod(access, name, desc, signature, exceptions));
        AddLocalVariableMethodVisitor addLocalVariableMethodVisitor = new AddLocalVariableMethodVisitor(classRef, access, desc, lineCoverage);
        addLocalVariableMethodVisitor.setLineConverageVisitor(lineCoverage);
        return addLocalVariableMethodVisitor;
        //return new BranchCoverageVisitor(access, desc, addLocalVariableMethodVisitor);
    }
}

