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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.nigori.common.MessageLibrary;

/**
 * Encapsulates communication with server via http
 * @author drt24
 *
 */
public class Http {
  public static final int UNAUTHORIZED = HttpURLConnection.HTTP_UNAUTHORIZED;

  private final String serverUrl;
  public Http(String serverUrl){
    this.serverUrl = serverUrl;
  }

  public HttpResponse post(String requestType, byte[] data) throws IOException {

    URL url = new URL(serverUrl + requestType);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    try {
      HttpURLConnection.setFollowRedirects(true);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Length", "" + data.length);
      conn.setRequestProperty("Content-Type", MessageLibrary.MIMETYPE_JSON);
      BufferedOutputStream out = new BufferedOutputStream(
          new DataOutputStream(conn.getOutputStream()));
      out.write(data);
      out.flush();

      conn.connect();

      final int response = conn.getResponseCode();
      final String message = conn.getResponseMessage();
      final InputStream input = conn.getInputStream();
      return new HttpResponse(response, message, input);
    } catch (FileNotFoundException fnfe) { //Occurs in the case of HTTP 404 response code
      final int response = conn.getResponseCode();
      final String message = conn.getResponseMessage();
      final InputStream input = conn.getErrorStream();
      return new HttpResponse(response, message, input);
    } catch (IOException ioe) { //Occurs in the case of HTTP 401 and others
      final int response = conn.getResponseCode();
      final String message = conn.getResponseMessage();
      final InputStream input = conn.getErrorStream();
      return new HttpResponse(response, message, input);      
    }
  }
}
