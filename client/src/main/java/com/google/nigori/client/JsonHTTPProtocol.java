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

import static com.google.nigori.common.MessageLibrary.toBytes;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.MessageLibrary.JsonConversionException;
import com.google.nigori.common.NigoriMessages.AuthenticateRequest;
import com.google.nigori.common.NigoriMessages.DeleteRequest;
import com.google.nigori.common.NigoriMessages.GetIndicesRequest;
import com.google.nigori.common.NigoriMessages.GetIndicesResponse;
import com.google.nigori.common.NigoriMessages.GetRequest;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.GetRevisionsRequest;
import com.google.nigori.common.NigoriMessages.GetRevisionsResponse;
import com.google.nigori.common.NigoriMessages.PutRequest;
import com.google.nigori.common.NigoriMessages.RegisterRequest;
import com.google.nigori.common.NigoriMessages.UnregisterRequest;
import com.google.nigori.common.NigoriProtocol;
import com.google.nigori.common.NotFoundException;
import com.google.nigori.common.UnauthorisedException;

/**
 * Implements the NigoriProtocol using Json and HTTP
 * 
 * @author drt24
 * 
 */
public class JsonHTTPProtocol implements NigoriProtocol {

  private static final boolean PRINTFAILRESPONSE = false;

  private final Http http;

  public JsonHTTPProtocol(String server, int port, String serverPrefix) {
    String protocol = "https://";
    if ("localhost".equals(server)) {
      protocol = "http://";
    }
    http = new Http(protocol + server + ":" + port + "/" + serverPrefix + "/");
  }

  /**
   * Get whether the HttpResponse indicates that the call was successful, if not print the message
   * if configured to do so.
   * 
   * @param resp
   * @return whether the {@link HttpResponse#getResponseCode()} indicates success
   */
  @SuppressWarnings("unused")
  // for debugging
  private static boolean success(HttpResponse resp) {
    boolean success = resp.getResponseCode() == HttpURLConnection.HTTP_OK;
    if (!success && PRINTFAILRESPONSE) {
      System.err.println(resp.getResponseCode() + " : " + resp.getResponseMessage() + " : "
          + inputStreamToString(resp.getInputStream()));
    }
    return success;
  }

  /**
   * Turn an input stream into a String or in the case of an IO exception during that process return
   * the empty string.
   * 
   * @param s
   * @return
   */
  private static String inputStreamToString(InputStream s) {
    if (s == null) {
      return "";
    }
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(s));
      StringBuilder builder = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        builder.append(line);
        builder.append("\n");
        line = reader.readLine();
      }
      return builder.toString();
    } catch (IOException e) {
      return "";
    }
  }

  @Override
  public boolean authenticate(AuthenticateRequest request) throws IOException {
    String json = MessageLibrary.toJson(request);
    HttpResponse resp = http.post(MessageLibrary.REQUEST_AUTHENTICATE, toBytes(json));
    return success(resp);
  }

  @Override
  public boolean register(RegisterRequest request) throws IOException {
    String json = MessageLibrary.toJson(request);
    HttpResponse resp = http.post(MessageLibrary.REQUEST_REGISTER, toBytes(json));
    return success(resp);
  }

  @Override
  public boolean unregister(UnregisterRequest request) throws IOException {
    String json = MessageLibrary.toJson(request);
    HttpResponse resp = http.post(MessageLibrary.REQUEST_UNREGISTER, toBytes(json));
    return success(resp);
  }

  private static void failure(Response response) throws IOException, UnauthorisedException{
    if (response.resp.getResponseCode() == Http.UNAUTHORIZED){
      throw new UnauthorisedException(response.jsonResponse);
    }
    throw new IOException("Server did not accept request(" + response.resp.getResponseCode()
        + "). " + response.jsonResponse);
  }
  @Override
  public GetResponse get(GetRequest request) throws IOException, UnauthorisedException {
    try {
      Response response = postResponse(MessageLibrary.REQUEST_GET, MessageLibrary.toJson(request));

      if (response.notFound()) {
        return null; // request was successful, but no data key by that name was found.
      }

      if (!success(response.resp)) {
        failure(response);
      }
      return MessageLibrary.getResponseFromJson(response.jsonResponse);
    } catch (JsonConversionException jce) {
      throw new IOException("Error reading JSON sent by server: " + jce.getMessage());
    }
  }

  @Override
  public GetIndicesResponse getIndices(GetIndicesRequest request) throws IOException, UnauthorisedException {
    try {
      Response response =
          postResponse(MessageLibrary.REQUEST_GET_INDICES, MessageLibrary.toJson(request));

      if (response.notFound()) {
        return null; // request was successful, but no data key by that name was found.
      }

      if (!success(response.resp)) {
        failure(response);
      }
      return MessageLibrary.getIndicesResponseFromJson(response.jsonResponse);
    } catch (JsonConversionException jce) {
      throw new IOException("Error reading JSON sent by server: " + jce.getMessage());
    }
  }

  @Override
  public GetRevisionsResponse getRevisions(GetRevisionsRequest request) throws IOException, NotFoundException, UnauthorisedException {
    try {
      Response response =
          postResponse(MessageLibrary.REQUEST_GET_REVISIONS, MessageLibrary.toJson(request));

      if (response.notFound()) {
        throw new NotFoundException(response.jsonResponse);// request was successful, but no data key by that name was found.
      }

      if (!success(response.resp)) {
        failure(response);
      }
      return MessageLibrary.getRevisionsResponseFromJson(response.jsonResponse);
    } catch (JsonConversionException jce) {
      throw new IOException("Error reading JSON sent by server: " + jce.getMessage());
    }
  }

  @Override
  public boolean put(PutRequest request) throws IOException {
    String json = MessageLibrary.toJson(request);
    HttpResponse resp = http.post(MessageLibrary.REQUEST_PUT, toBytes(json));
    return success(resp);
  }

  @Override
  public boolean delete(DeleteRequest request) throws IOException, UnauthorisedException {
    Response response = postResponse(MessageLibrary.REQUEST_DELETE, MessageLibrary.toJson(request));

    if (response.notFound()) {
      return false; // request was successful, but no data key by that name was found.
    }

    if (!success(response.resp)) {
      failure(response);
    }
    return true;
  }

  private Response postResponse(String request, String jsonRequest)
      throws UnsupportedEncodingException, IOException {

    HttpResponse resp = http.post(request, toBytes(jsonRequest));

    BufferedInputStream in = new BufferedInputStream(resp.getInputStream());
    StringBuilder jsonResponse = new StringBuilder();
    // TODO(beresford): optimise this buffer size
    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
      jsonResponse.append(new String(buffer, 0, bytesRead, MessageLibrary.CHARSET));
    }
    return new Response(resp, jsonResponse.toString());
  }

  private static class Response {
    public final HttpResponse resp;
    public final String jsonResponse;

    public Response(HttpResponse resp, String jsonResponse) {
      this.resp = resp;
      this.jsonResponse = jsonResponse;
    }

    public boolean notFound() {
      return (resp.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND);
    }
  }
}
