/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.helpers;

import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.slf4j.LoggerFactory.getLogger;

public class HostHelper {
    private static final Logger logger = getLogger(HostHelper.class);

    private static String hostname;
    private static String shortName;
    private static String hostnameGroup;
    private static String shortNameGroup;
    private static String hostIP;

    static {
        try {
            hostIP = InetAddress.getLocalHost().getHostAddress();
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
            shortName = hostname.split("\\.")[0];
            hostnameGroup = hostname.replaceAll("-?[0-9]+\\.", ".");
            shortNameGroup = hostnameGroup.split("\\.")[0];
        } catch (UnknownHostException e) {
            logger.warn("Unable to get hostname", e);
        }
    }

    public static String getHostName(String defaultValue) {
        return hostname == null ? defaultValue : hostname;
    }

    public static String getHostGroup(String defaultValue) {
        return hostnameGroup == null ? defaultValue : hostnameGroup;
    }

    public static String getShortHostName(String defaultValue) {
        return shortName == null ? defaultValue : shortName;
    }

    public static String getShortHostGroup(String defaultValue) {
        return shortNameGroup == null ? defaultValue : shortNameGroup;
    }

    public static String getHostIP(String defaultValue) {
        return hostIP == null ? defaultValue : hostIP;
    }
}
