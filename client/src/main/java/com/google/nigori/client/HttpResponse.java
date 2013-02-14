/*
 * Copyright (C) 2012 Daniel Thomas (drt24)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.nigori.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.nigori.common.MessageLibrary;

/**
 * Encapsulates response from server
 * 
 * @author drt24
 * 
 */
class HttpResponse {
  private static Logger log = Logger.getLogger(HttpResponse.class.getCanonicalName());

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

  String getResponseMessage() {
    return responseMessage;
  }

  public String toOutputString() throws IOException {
    try {
      BufferedInputStream in = new BufferedInputStream(input);
      StringBuilder jsonResponse = new StringBuilder();
      // TODO(beresford): optimise this buffer size
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        jsonResponse.append(new String(buffer, 0, bytesRead, MessageLibrary.CHARSET));
      }
      return jsonResponse.toString();
    } finally {
      input.close();
    }
  }

  @Override
  public String toString() {

    try {
      StringBuilder buffer = new StringBuilder();
      buffer.append("Response: " + getResponseCode() + " (" + getResponseMessage() + ")\n");

      BufferedReader reader;
      try {
        reader = new BufferedReader(new InputStreamReader(input, MessageLibrary.CHARSET));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);// never happens as UTF-8 is supported
      }
      String line;
      try {
        if (reader != null) {
          while ((line = reader.readLine()) != null) {
            buffer.append(line);
          }
          reader.close();
        }
      } catch (IOException e) {
        buffer.append("*** read interrupted: " + e);
      }

      return buffer.toString();
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        log.log(Level.WARNING, "IOE while turning response to string", e);
      }
    }
  }

  public void close() throws IOException {
    input.close();
  }
}