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

package com.flipkart.poseidon.jmx;

import org.apache.commons.lang.exception.ExceptionUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.EOFException;

/**
 * A standalone program to invoke mbean operations over JMX on a running Poseidon process.
 * Can be used to shutdown a Poseidon application cleanly.
 * Ex: PoseidonJMXInvoker &lt;host&gt; &lt;port&gt; destroy.
 *
 * Created by mohan.pandian on 12/07/16.
 */
public class PoseidonJMXInvoker {
    // BootstrapModelMBeanExporter.MBEAN_NAME is private in Trooper.
    private static final String MBEAN_NAME = "spring.application:type=Trooper,application=Runtime,name=Bootstrap-poseidon";

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println(PoseidonJMXInvoker.class.getSimpleName() + " <host> <port> <operation>");
            System.exit(-1);
        }

        final String CONNECT_STRING = args[0] + ":" + args[1];
        final String OPERATION = args[2];
        try {
            System.out.println("Running " + OPERATION + " over JMX on " + CONNECT_STRING);

            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + CONNECT_STRING + "/jmxrmi");
            MBeanServerConnection connection = JMXConnectorFactory.connect(url).getMBeanServerConnection();
            connection.invoke(ObjectName.getInstance(MBEAN_NAME), OPERATION, null, null);
        } catch (Exception e) {
            if (!(ExceptionUtils.getRootCause(e) instanceof EOFException && "destroy".equals(OPERATION))) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        System.out.println(OPERATION + " successful over JMX on " + CONNECT_STRING);
    }
}
