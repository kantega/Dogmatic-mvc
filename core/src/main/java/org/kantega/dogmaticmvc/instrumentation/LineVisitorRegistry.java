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

import java.util.BitSet;
import java.util.Arrays;

/**
 */
public class LineVisitorRegistry {
    public static int totalNumberOfClasses;


    public static int maxClasses = 0;
    public static int maxLines = 0;

    public static final int numClasses = 10000;

    public static int[][] lineCounts = new int[numClasses][];


    public static String[] classNames = new String[numClasses];


    static {
        System.out.println("Initializing " + LineVisitorRegistry.class.getName());
    }

    public synchronized static int getRegisteredClass(String className) {
        for(int i = 0; i < classNames.length; i++) {
            if(className.equals(classNames[i])) {
                return i;
            }
        }
        return -1;
    }
    public synchronized static int registerClass(String className, BitSet instructionLines, String[] methodnames) {
        classNames[maxClasses] = className;
        lineCounts[maxClasses] = new int[instructionLines.length()];
        maxLines = Math.max(maxLines, instructionLines.length());
        Arrays.fill(lineCounts[maxClasses], -1);

        for(int i=instructionLines.nextSetBit(0); i>=0; i=instructionLines.nextSetBit(i+1)) {
            lineCounts[maxClasses][i] = 0;
        }
        return maxClasses++;
    }

}