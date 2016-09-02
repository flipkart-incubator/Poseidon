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

package com.flipkart.poseidon.log4j.pattern;

import com.flipkart.poseidon.log4j.Log4JAccessLog;
import com.flipkart.poseidon.log4j.message.AccessLog;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.function.Supplier;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Unit tests for Log4JAccessLog
 *
 * Created by mohan.pandian on 02/09/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Log4JAccessLog.class })
public class Log4JAccessLogTest {
    // Constants
    private static final String ACCESS_CONFIG_FILE_PATH = "log4j-access.xml";
    private static final String NON_EXISTING_FILE_PATH = "dummy.xml";

    // Objects to be mocked
    private Log4jContextFactory mockLog4jContextFactory;
    private LoggerContext mockLoggerContext;
    private Logger mockLogger;
    private Request mockRequest;
    private Response mockResponse;
    private AccessLog mockAccessLog;

    // Actual objects to test
    private Supplier enabledSupplier;
    private Supplier disabledSupplier;
    private TestLog4JAccessLog failedAccessLog;
    private TestLog4JAccessLog enabledAccessLog;
    private TestLog4JAccessLog disabledAccessLog;

    @Before
    public void setup() throws Exception {
        // Create mock objects
        mockLog4jContextFactory = mock(Log4jContextFactory.class);
        whenNew(Log4jContextFactory.class).withNoArguments().thenReturn(mockLog4jContextFactory);
        mockLoggerContext = mock(LoggerContext.class);
        mockLogger = mock(Logger.class);
        when(mockLog4jContextFactory.getContext(anyString(), any(ClassLoader.class), any(), anyBoolean(), any(URI.class), anyString())).thenReturn(mockLoggerContext);
        when(mockLoggerContext.getRootLogger()).thenReturn(mockLogger);
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        mockAccessLog = mock(AccessLog.class);
        whenNew(AccessLog.class).withArguments(mockRequest, mockResponse).thenReturn(mockAccessLog);

        // Create actual objects
        enabledSupplier = () -> true;
        disabledSupplier = () -> false;
        failedAccessLog = new TestLog4JAccessLog(NON_EXISTING_FILE_PATH, enabledSupplier);
        String filePath = getClass().getClassLoader().getResource(ACCESS_CONFIG_FILE_PATH).getPath();
        enabledAccessLog = new TestLog4JAccessLog(filePath, enabledSupplier);
        disabledAccessLog = new TestLog4JAccessLog(filePath, disabledSupplier);
    }

    /**
     * Tests multiple cases
     * 1. If log4j access config file doesn't exist, should throw exception
     * 2. If log4j access config file exists, shouldn't throw exception
     * 3. If logging is enabled, log() should get called
     * 4. If logging is disabled, log() shouldn't get called
     *
     * @throws Exception
     */
    @Test
    public void testLog4JAccessLog() throws Exception {
        // Config file doesn't exist, should throw exception
        try {
            failedAccessLog.startLog();
            fail("Non existing access log; should fail, but didn't");
        } catch(Exception e) {
            if (!e.getMessage().contains("Log4J access config file not found")) {
                fail("Non existing access log; but failed with some other error: " + e.getMessage());
            }
        }

        // Config file exists, logging enabled
        try {
            enabledAccessLog.startLog();
        } catch(Exception e) {
            fail("Existing access log; shouldn't fail: " + e.getMessage());
        }
        enabledAccessLog.log(mockRequest, mockResponse);
        verify(mockLogger, times(1)).log(Level.INFO, mockAccessLog);

        // Config file exists, but logging disabled
        try {
            disabledAccessLog.startLog();
        } catch (Exception e) {
            fail("Existing access log; shouldn't fail: " + e.getMessage());
        }
        disabledAccessLog.log(mockRequest, mockResponse);
        verify(mockLogger, times(1)).log(Level.INFO, mockAccessLog);
    }

    /**
     * Test class extending {@link Log4JAccessLog} to call doStart() in test cases
     * as it is protected in Log4JAccessLog
     */
    private static class TestLog4JAccessLog extends Log4JAccessLog {
        public TestLog4JAccessLog(String accessConfigFilePath, Supplier<Boolean> isEnabledSupplier) {
            super(accessConfigFilePath, isEnabledSupplier);
        }

        public void startLog() throws Exception {
            doStart();
        }
    }
}
