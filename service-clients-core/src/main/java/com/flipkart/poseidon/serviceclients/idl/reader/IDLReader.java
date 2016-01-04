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

package com.flipkart.poseidon.serviceclients.idl.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.poseidon.serviceclients.idl.pojo.ServiceIDL;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by mohan.pandian on 20/02/15.
 *
 * Reads the service IDL file and converts it to ServiceIDL pojo using jackson
 */
public class IDLReader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ServiceIDL getIDL(String filePath) throws IOException {
        String json = FileUtils.readFileToString(new File(filePath));
        return convertToIDL(json);
    }

    public static ServiceIDL convertToIDL(String json) throws IOException {
        return mapper.readValue(json, ServiceIDL.class);
    }
}
