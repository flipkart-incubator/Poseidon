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

package com.flipkart.poseidon.validator;

import java.util.List;
import java.util.Map;

/**
 * Created by shrey.garg on 06/07/16.
 */
public class ValidatorUtils {
    public static boolean isEmpty(Object value) {
        if (value instanceof List) {
            return ((List) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map) value).isEmpty();
        } else if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        return false;
    }

    public static String getPrintableCSVs(List list) {
        if (list.size() < 1) {
            return "";
        } else if (list.size() == 1) {
            return String.valueOf(list.get(0));
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size() - 2; i++) {
            builder.append(list.get(i)).append(", ");
        }
        builder.append(list.get(list.size() - 2)).append(" and ").append(list.get(list.size() - 1));
        return builder.toString();
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String stripBraces(String value) {
        if (value.length() < 5) {
            return value;
        }

        return value.substring(2, value.length() - 2);
    }

    public static String braced(String value) {
        return "{{ " + value + " }}";
    }

    public static String getFormattedErrorMessages(Map<String, List<String>> errors) {
        StringBuilder builder = new StringBuilder("\n\n");
        for (Map.Entry<String, List<String>> entry: errors.entrySet()) {
            builder.append("--------------------------------------------").append("\n");
            builder.append("Errors while validating ").append(entry.getKey()).append("\n");
            builder.append("--------------------------------------------").append("\n");
            entry.getValue().forEach(builder::append);
            builder.append("\n").append("\n").append("\n");
        }
        return builder.toString();
    }
}
