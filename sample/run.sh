#!/usr/bin/env bash

JAVA_ARGS="-Dcom.sun.management.jmxremote.port=3335 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"

java ${JAVA_ARGS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -cp "target/*:target/lib/*:src/main/resources" -Dlog4j.configurationFile=src/main/resources/external/log4j.xml -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector com.flipkart.poseidon.Poseidon src/main/resources/external/bootstrap.xml
