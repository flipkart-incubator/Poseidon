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

import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Trace;
import flipkart.lego.api.entities.Block;

/**
 * Contains helpers methods like to get name of lego blocks like
 * datasources, filters etc
 *
 * Created by mohan.pandian on 31/05/16.
 */
public class BlocksHelper {
    public static String getName(Block block) {
        Name name = block.getClass().getDeclaredAnnotation(Name.class);
        return name.value();
    }

    public static boolean trace(Block block) {
        Trace trace = block.getClass().getDeclaredAnnotation(Trace.class);
        if (trace != null) {
            return trace.value();
        }
        return true;
    }

}
