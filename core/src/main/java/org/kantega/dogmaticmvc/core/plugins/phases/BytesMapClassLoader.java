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

import java.util.Map;

/**
 *
 */
public class BytesMapClassLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    public BytesMapClassLoader(ClassLoader classLoader, Map<String, byte[]> classes) {
        super(classLoader);
        this.classes = classes;
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) {
            return clazz;
        }


        try {
            clazz = findClass(name);
        } catch (ClassNotFoundException e) {
            clazz = getParent().loadClass(name);
        }

        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;


    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (classes.containsKey(name)) {
            byte[] bytes = classes.get(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
        throw new ClassNotFoundException(name);
    }
}
