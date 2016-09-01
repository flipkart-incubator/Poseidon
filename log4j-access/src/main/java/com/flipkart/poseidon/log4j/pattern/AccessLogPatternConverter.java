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

import com.flipkart.poseidon.log4j.message.IAccessLog;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.message.Message;

/**
 * AccessLogPatternConverter extends {@link LogEventPatternConverter} to
 * convert keys in pattern (like %statusCode) to actual values from {@link IAccessLog}
 * <p/>
 * Created by mohan.pandian on 31/08/16.
 */
public abstract class AccessLogPatternConverter extends LogEventPatternConverter {
    protected AccessLogPatternConverter() {
        super(null, null);
    }

    /**
     * Formats LogEvent into a string builder.
     *
     * @param logEvent      event to format, may not be null.
     * @param stringBuilder string builder to which the formatted event will be appended.
     */
    @Override
    public void format(LogEvent logEvent, StringBuilder stringBuilder) {
        Message message = logEvent.getMessage();
        if (message instanceof IAccessLog) {
            format((IAccessLog) message, stringBuilder);
        }
    }

    /**
     * Formats IAccessLog into a string builder.
     *
     * @param accessLog IAccessLog to get access log details like request URL, status code, time elapsed etc.
     * @param stringBuilder string builder to which the formatted event will be appended.
     */
    protected abstract void format(IAccessLog accessLog, StringBuilder stringBuilder);
}
