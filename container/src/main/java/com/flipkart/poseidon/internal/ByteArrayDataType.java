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

package com.flipkart.poseidon.internal;

import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;

import flipkart.lego.api.entities.DataType;

@Name("ByteArrayDataType")
@Version(major = 1, minor = 0, patch = 0)
@Description("DataType wrapper for byte array")
public class ByteArrayDataType implements DataType {

	private static final long serialVersionUID = 1L;

	private byte[] rawBytes;

	private boolean skipDefaultContentType;

	public ByteArrayDataType(byte[] rawBytes) {
		this.rawBytes = rawBytes;
	}

	public byte[] getRawBytes() {
		return rawBytes;
	}

	public boolean skipDefaultContentType() {
		return skipDefaultContentType;
	}

	public ByteArrayDataType(byte[] rawBytes, boolean skipDefaultContentType) {
		this.rawBytes = rawBytes;
		this.skipDefaultContentType = skipDefaultContentType;
	}

}
