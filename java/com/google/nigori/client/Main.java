/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines a command line interface for interacting with a Nigori server.
 *
 * @author Alastair Beresford
 */
public class Main {

  private final static SecureRandom random = new SecureRandom();

  private static class JsonRecordHolder {
    int version;
    String creationTime;
    String name;
    String value;
  }

  private static class HttpResponse {
    int code;
    String message;
    BufferedReader reader;
    public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Response: "+code+" ("+message+")");
      String line;
      try {
        if (reader != null) {
          while((line = reader.readLine()) != null) {
            buffer.append(line);
          }
          reader.close();
        }
      } catch(IOException e) {
        buffer.append("*** interrupted: " + e);
      }
      return buffer.toString();
    }
  }

  private static HttpResponse download(URLConnection conn) throws IOException {
    HttpResponse response = new HttpResponse();

    if (conn instanceof HttpURLConnection) {

      HttpURLConnection http = (HttpURLConnection) conn;
      response.code = http.getResponseCode();
      response.message = http.getResponseMessage();
      if (response.message == null)
        response.message = "[No message provided]";

      InputStream in;
      if (response.code < 400) {
        in = http.getInputStream();
      } else {
        in = http.getErrorStream();
      }
      
      if (in == null) {
        response.reader = null;
      } else {
        response.reader = new BufferedReader(new InputStreamReader(in));
      }
    } else {
      //TODO(beresford): Generate/throw some kind of error.
      response.message = "Response code does not exist (not an HTTP connection!)";
    }
    return response;

  }

  private static HttpResponse httpGet(String url, Map<String, String> parameters) 
  throws IOException {

    //TODO(beresford): No Java SE support for query string encoding. Could replace using Apache
    //Http Components, but updates to message spec will delete this piece of code soon anyway.
    StringBuffer buf = new StringBuffer(url);
    buf.append('?');
    for(Map.Entry<String, String> element: parameters.entrySet()) {
      buf.append(element.getKey());
      buf.append('=');
      buf.append(element.getValue());
      buf.append('&');
    }
    URLConnection conn = new URL(buf.toString()).openConnection();

    return download(conn);
  }

  private static HttpResponse httpPost(String url, Map<byte[], byte[]> postData) 
  throws IOException {

    URLConnection conn = new URL(url).openConnection();

    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    conn.setDoOutput(true);
    conn.setUseCaches(false);

    BufferedOutputStream data = new BufferedOutputStream(conn.getOutputStream());
    for(Map.Entry<byte[],byte[]> element: postData.entrySet()) {
      data.write(element.getKey());
      data.write('=');
      data.write(element.getValue());
      data.write('&');
    }
    data.flush();
    data.close();

    return download(conn);
  }

  private static void register(String url, byte[] servername, byte[] username, byte[] password) 
  throws KeyManagerCryptographyException, IOException {

    byte[] publicKey = new KeyManager(servername, username, password).signer().getPublicKey();

    HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    map.put("user".getBytes(),username);
    map.put("publicKey".getBytes(), Util.encodeBase64UrlSafe(publicKey));

    System.out.println(httpPost(url + "register", map));
  }

  private static void authenticate(String url, String user, SchnorrSignature sig) 
  throws IOException {

    //TODO(beresford): There are charset issues with using this (i.e. if UTF-8 isn't available)
    // This problem will be fixed when we shift to the new message format.
    HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    map.put("user".getBytes(), URLEncoder.encode(user, "UTF-8").getBytes());
    map.put("t".getBytes(), URLEncoder.encode(new String(sig.getMessage()), "UTF-8").getBytes());
    map.put("e".getBytes(), Util.encodeBase64UrlSafe(sig.getE()));
    map.put("s".getBytes(), Util.encodeBase64UrlSafe(sig.getS()));

    System.out.println(httpPost(url + "authenticate", map));
  }

  private static void add(String url, String user, byte[] servername, byte[] username, 
      byte[] password, byte[] name, byte[] value) 
  throws KeyManagerCryptographyException, NoSuchAlgorithmException, IOException {

    SchnorrSignature sig = makeAuthParams(servername, username, password);
    KeyManager keyManager = new KeyManager(servername, username, password);

    final HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    map.put("user".getBytes(), URLEncoder.encode(user,"UTF-8").getBytes());
    map.put("t".getBytes(), URLEncoder.encode(new String(sig.getMessage()),"UTF-8").getBytes());
    map.put("e".getBytes(), Util.encodeBase64UrlSafe(sig.getE()));
    map.put("s".getBytes(), Util.encodeBase64UrlSafe(sig.getS()));

    //TODO: Type is hard-coded to "1" here as in Python client; should make this an option
    final byte[] nameAndType = Util.concatAndPrefix(new byte[][]{new byte[]{1},name});		
    final byte[] encryptedName = keyManager.permute(nameAndType);
    map.put("name".getBytes(), Util.encodeBase64UrlSafe(encryptedName));

    final byte[] encryptedValue = keyManager.encrypt(value);
    map.put("value".getBytes(), Util.encodeBase64UrlSafe(encryptedValue));

    System.out.println(httpPost(url+"add-resource", map));
  }

  private static void get(String url, String user, byte[] servername, byte[] username, 
      byte[] password, byte[] name) 
  throws KeyManagerCryptographyException, NoSuchAlgorithmException, IOException {

    SchnorrSignature sig = makeAuthParams(servername, username, password);
    KeyManager keyManager = new KeyManager(servername, username, password);

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", URLEncoder.encode(user, "UTF-8"));
    map.put("t", URLEncoder.encode(new String(sig.getMessage()), "UTF-8"));
    map.put("e", new String(Util.encodeBase64UrlSafe(sig.getE())));
    map.put("s", new String(Util.encodeBase64UrlSafe(sig.getS())));

    //TODO(beresford): Type is hard-coded to "1" here as in Python client; make this an option
    byte[] nameAndType = Util.concatAndPrefix(new byte[][]{new byte[]{1}, name});		
    byte[] encryptedName = keyManager.permute(nameAndType);
    String encryptedNameString = new String(Util.encodeBase64UrlSafe(encryptedName));
    map.put("name", encryptedNameString);

    Gson gson = new Gson();
    JsonRecordHolder[] records = gson.fromJson(httpGet(url + "list-resource", map).reader, 
        JsonRecordHolder[].class);
    for(int i = 0; i < records.length; i++) {
      byte[] value = keyManager.decrypt(Util.decodeBase64UrlSafe(records[i].value.getBytes()));
      System.out.println(new String(name) + ": " + new String(value));
      System.out.println(" [" + records[i].creationTime + " " +
          records[i].version + " " + records[i].name + "]");
    }
  }

  private static SchnorrSignature makeAuthParams(byte[] servername, byte[] username, 
      byte[] password) 
  throws KeyManagerCryptographyException, NoSuchAlgorithmException {

    int r = random.nextInt();
    //TODO(beresford): Python implementation has only 20-bits of randomness. Important?
    r &= (1 << 20) - 1;
    int sinceEpoch = (int) (System.currentTimeMillis() / 1000);
    //TODO(beresford): Should specify charset (e.g. "UTF-8") throughout, but this requires charset
    //handling of not found errors. This problem goes away with new JSON format, so ignored here.
    byte[] token = ("" + sinceEpoch + ":" + r).getBytes();

    KeyManager key = new KeyManager(servername, username, password);
    SchnorrSign schnorr = key.signer();
    return schnorr.sign(token);
  }

  private static void usage() {
    System.out.println("Usage: java nigori.jar server port <action and args>");
    System.out.println(" Where <action and args> is one of the following:");
    System.out.println("  register username password");
    System.out.println("  authenticate username password");
    System.out.println("  add username password key value");
    System.out.println("  get username password key");		
  }

  public static void main(String[] args) throws Exception {

    if (args.length < 5 || args.length > 8) {
      usage();
      return;
    }

    final String server = args[0];
    final int port = Integer.parseInt(args[1]);
    final String action = args[2];
    final String url = "http://" + server + ":" + port + "/";
    final String user = args[3];
    final byte[] servername = (server + ":" + port).getBytes("UTF-8");
    final byte[] username = args[3].getBytes("UTF-8");
    final byte[] password = args[4].getBytes("UTF-8");

    if (action.equals("register")) {
      register(url, servername, username, password);
    }
    else if (action.equals("authenticate")) {
      SchnorrSignature sig = makeAuthParams(servername, username, password);
      authenticate(url, user, sig);
    }
    else if (action.equals("add")) {
      if (args.length != 7) {
        System.out.println("*** Error: exactly six elements needed for a get action");
        usage();
        return;
      }
      add(url, user, servername, username, password, args[5].getBytes(), args[6].getBytes());
    }
    else if (action.equals("get")) {
      if (args.length != 6) {
        System.out.println("*** Error: exactly six elements needed for a get action");
        usage();
        return;
      }
      get(url, user, servername, username, password, args[5].getBytes("UTF-8"));
    }

    else {
      System.out.println("*** Error: Unknown action "+action);
      usage();
    }
  }
}
