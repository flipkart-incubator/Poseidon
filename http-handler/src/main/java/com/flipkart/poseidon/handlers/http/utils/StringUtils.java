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

package com.flipkart.poseidon.handlers.http.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class StringUtils {
	
	/**
	 * Returns false if given string in null or empty
	 */
	public static boolean isNullOrEmpty(String string) {
		if(string != null) {
			return string.isEmpty();
		}
		return true;
	}

	/**
	 *  Joins the array of values with a given separator and returns the .
	 */
	public static String join(String[] arrayOfStrings, String separator) {
		boolean appendSeparator = false;
		if(arrayOfStrings!=null) {
			StringBuilder stringBuilder = new StringBuilder();
			for(String string : arrayOfStrings) {
				if(appendSeparator) {
					stringBuilder.append(separator);
				}
				else {
					appendSeparator=true;
				}
				stringBuilder.append(string);
			}
			return stringBuilder.toString();
		}
		else {
			throw new NullArgumentException("arrayOfStrings");
		}
	}

	/**
	 * always encode in UTF8. does NOT close your input stream. client need to close it.
	 * @param inputStream
	 * @return String encoded using UTF-8 scheme.
	 * @throws Exception, any exception encountered. Will not check for any null condition.
	 */
	public static String inputStream2String(InputStream inputStream) throws Exception {
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, "UTF-8");
		String theString = writer.toString();
		return theString;
	}

	/**
	 * Gets the query parameters from the URL
	 * @param httpUrl The URL from which query params have to be extracted
	 * @return 
	 */
	public static Map<String, String> getQueryParams(String httpUrl) {
		Map<String, String> params = new HashMap<String, String>();
		if (httpUrl == null) {
			return params;
		}

		URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		String query = url.getQuery();
		if (query == null) {
			return params;
		}

		StringTokenizer tokenizer = new StringTokenizer(query, "&");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf("=");
			params.put(token.substring(0, index).trim(), token.substring(index + 1).trim());
		}

		return params;
	}

    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append((line + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}