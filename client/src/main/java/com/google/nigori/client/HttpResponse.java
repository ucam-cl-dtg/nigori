/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
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
package com.google.nigori.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Encapsulates response from server
 * @author drt24
 *
 */
class HttpResponse {

	private int responseCode;
	private String responseMessage;
	private InputStream input;

	HttpResponse(int responseCode, String responseMessage, InputStream input) {
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
		this.input = input;
	}

	int getResponseCode() {
		return responseCode;
	}

	InputStream getInputStream() {
		return input;
	}

	String getResponseMessage() {
		return responseMessage;
	}

	public String toString() {

		StringBuilder buffer = new StringBuilder();
		buffer.append("Response: " + getResponseCode() + " (" + getResponseMessage() + ")\n");

		BufferedReader reader = new BufferedReader(new InputStreamReader(input));      
		String line;
		try {
			if (reader != null) {
				while((line = reader.readLine()) != null) {
					buffer.append(line);
				}
				reader.close();
			}
		} catch(IOException e) {
			buffer.append("*** read interrupted: " + e);
		}

		return buffer.toString();
	}
}