/*
 * Copyright 2016 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.poseidon.helper;

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by shrey.garg on 15/07/16.
 */
public class ClassPathHelper {
    public static Set<ClassPath.ClassInfo> getPackageClasses(ClassLoader classLoader, List<String> packagesToScan) throws IOException {
        ClassPath classpath = ClassPath.from(classLoader);
        Set<ClassPath.ClassInfo> classInfos = new HashSet<>();
        for (String basePackage : packagesToScan) {
            classInfos.addAll(classpath.getTopLevelClassesRecursive(basePackage));
        }
        return classInfos;
    }
}
