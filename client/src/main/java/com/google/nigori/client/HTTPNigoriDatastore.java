/*
 * Copyright (C) 2011 Google Inc.
 * Copyright (C) 2011 Alastair R. Beresford
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.google.nigori.common.Index;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.Revision;
import com.google.nigori.common.MessageLibrary.JsonConversionException;
import com.google.nigori.common.NigoriMessages.GetIndicesResponse;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.GetRevisionsResponse;
import com.google.nigori.common.NigoriMessages.RevisionValue;
import com.google.nigori.common.RevValue;
import com.google.protobuf.ByteString;

/**
 * A client API capable of managing a session with a Nigori Server.
 * 
 * @author Alastair Beresford
 *
 * It is worth looking at {@link java.util.Collection} but we can't implement that or most of the methods in it until we have a "list indexes" method which at least for now we don't intend to do.
 * putAll from {@link java.util.Map} might be worth implementing
 */
public class HTTPNigoriDatastore implements NigoriDatastore {

	private final String serverUrl;
	private final KeyManager keyManager;
	private static final boolean PRINTFAILRESPONSE = true;

	private class HttpResponse {

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

  /**
   * Get whether the HttpResponse indicates that the call was successful, if not print the message if configured to do so.
   * @param resp
   * @return whether the {@link HttpResponse#getResponseCode()} indicates success
   */
  private static boolean success(HttpResponse resp){
    boolean success = resp.getResponseCode() == HttpURLConnection.HTTP_OK;
    if (!success && PRINTFAILRESPONSE) {
      System.err.println(resp.getResponseCode() + " : " + resp.getResponseMessage() + " : " + inputStreamToString(resp.getInputStream()));
    }
    return success;
  }

  /**
   * Turn an input stream into a String or in the case of an IO exception during that process return the empty string.
   * @param s
   * @return
   */
  private static String inputStreamToString(InputStream s) {
    if (s == null){
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

	private HttpResponse post(String requestType, byte[] data) throws IOException {

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
		}	catch (IOException ioe) { //Occurs in the case of HTTP 401 and others
			final int response = conn.getResponseCode();
			final String message = conn.getResponseMessage();
			final InputStream input = conn.getErrorStream();
			return new HttpResponse(response, message, input);			
		}
	}


  /**
	 * Represents communication with a Nigori datastore for a specific user.
	 * 
	 * @param server DNS name or IP Address of the server.
	 * @param port Port number the service is running on.
	 * @param serverPrefix URI path on the server for the Nigori service.
	 * @param username name of account used to communicate with the Nigori service.
	 * @param password password of the account used to communicate with the Nigori service.
	 * @throws UnsupportedEncodingException if UTF-8 is unavailable on this platform.
	 * @throws NigoriCryptographyException if appropriate cryptography libraries are unavailable.
	 */
	public HTTPNigoriDatastore(String server, int port, String serverPrefix, String username,
			String password) throws NigoriCryptographyException, UnsupportedEncodingException {
		String servername = server + ":" + port;
		this.serverUrl = "http://" + server + ":" + port + "/" + serverPrefix + "/";
		keyManager = new RealKeyManager(servername.getBytes(MessageLibrary.CHARSET),
				username.getBytes(MessageLibrary.CHARSET),
				password.getBytes(MessageLibrary.CHARSET));
	}

	/**
	 * Represents communication with a Nigori datastore for a newly created user.
	 * 
	 * The username and password for the new user are generated automatically, and can be retrieved
	 * by calling getUsername and getPassword on this object.
	 * 
	 * @param server DNS name or IP Address of the server.
	 * @param port Port number the service is running on.
	 * @param serverPrefix URI path on the server for the Nigori service.
	 * @throws UnsupportedEncodingException if MessageLibrary.CHARSET is unavailable on this platform.
	 * @throws NigoriCryptographyException if appropriate cryptography libraries are unavailable.
	 */
	public HTTPNigoriDatastore(String server, int port, String serverPrefix) throws 
	NigoriCryptographyException, UnsupportedEncodingException {
		String servername = server + ":" + port;
		this.serverUrl = "http://" + server + ":" + port + "/" + serverPrefix + "/";

		keyManager = new RealKeyManager(servername.getBytes(MessageLibrary.CHARSET));
	}

	/**
	 * Retrieve the username used to connect with the Nigori datastore.
	 * 
	 * @return the username.
	 */
	public String getUsername() {
		try {
			return new String(keyManager.getUsername(), MessageLibrary.CHARSET);
		} catch (UnsupportedEncodingException uee) {
			return new String(keyManager.getUsername());
		}
	}

	/**
	 * Retrieve the password used to connect with the Nigori datastore.
	 * 
	 * @return the password.
	 */
	public String getPassword() {
		try {
			return new String(keyManager.getPassword(), MessageLibrary.CHARSET);
		} catch (UnsupportedEncodingException uee) {
			return new String(keyManager.getPassword());
		}		
	}

	/**
	 * Retrieve the public key associated with the username and password.
	 * 
	 * @return the public key.
	 */
	public byte[] getPublicKey() {
		return keyManager.signer().getPublicKey();
	}

	@Override
	public boolean register() throws IOException, NigoriCryptographyException {

		try{
		  byte[] token = {};
			String json = MessageLibrary.registerRequestAsJson(keyManager.signer(), token);
			HttpResponse resp = post(MessageLibrary.REQUEST_REGISTER,
					json.getBytes(MessageLibrary.CHARSET));
			return success(resp);
		} catch (NoSuchAlgorithmException e) {
			throw new NigoriCryptographyException("Platform does have required crypto support:" +
					e.getMessage());
		}
	}

	@Override
  public boolean unregister() throws IOException, NigoriCryptographyException {

    try{
      String json = MessageLibrary.unregisterRequestAsJson(keyManager.signer());
      HttpResponse resp = post(MessageLibrary.REQUEST_UNREGISTER,
          json.getBytes(MessageLibrary.CHARSET));
      return success(resp);
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException("Platform does have required crypto support:" +
          e.getMessage());
    }
  }

	@Override
	public boolean authenticate() throws IOException, NigoriCryptographyException {

		try {
			String json = MessageLibrary.authenticateRequestAsJson(keyManager.signer());
			HttpResponse resp = post(MessageLibrary.REQUEST_AUTHENTICATE, 
					json.getBytes(MessageLibrary.CHARSET));
			return success(resp);
		} catch (NoSuchAlgorithmException e) {
			throw new NigoriCryptographyException("Platform does have required crypto support:" +
					e.getMessage());
		}
	}

	@Override
	public boolean put(Index index, Revision revision, byte[] value)  throws IOException, NigoriCryptographyException {
		return put(null, index, revision, value);
	}

	/**
	 * Insert a new key-value pair into the datastore of the server.
	 * 
	 * @param key the key
	 * @param value the data value associated with the key.
	 * @param readAuthorities list of public keys of people permitted to read this key-value pair.
	 * @param writeAuthorities list of public keys of people permitted to read this key-value pair.
	 * @return true if the data was successfully inserted; false otherwise.
	 */
	private boolean put(byte[] encKey, Index index, Revision revision, byte[] value) throws IOException, 
	NigoriCryptographyException {

		byte[] encIndex;
		byte[] encRevision;
		byte[] encValue;
		if (encKey == null) {
			encIndex = keyManager.encryptDeterministically(index.getBytes());
			encRevision = keyManager.encryptDeterministically(revision.getBytes());
			encValue = keyManager.encrypt(value);
		} else {
			encIndex = keyManager.encryptDeterministically(encKey, index.getBytes());
			encRevision = keyManager.encryptDeterministically(encKey, revision.getBytes());
			encValue = keyManager.encrypt(encKey, value);
		}

		try {
			String json = MessageLibrary.putRequestAsJson(keyManager.signer(), encIndex, encRevision, encValue);
			HttpResponse resp = post(MessageLibrary.REQUEST_PUT, json.getBytes(MessageLibrary.CHARSET));
			return success(resp);
		} catch (NoSuchAlgorithmException e) {
			throw new NigoriCryptographyException("Platform does have required crypto support:" +
					e.getMessage());
		}
	}

	@Override
	public List<RevValue> get(Index index) throws IOException,	NigoriCryptographyException {
		return get(null, index, null);
	}

	/**
   * @param index
   * @param revision
   * @return
   * @throws NigoriCryptographyException 
   * @throws IOException 
   */
	@Override
  public byte[] getRevision(Index index, Revision revision) throws IOException, NigoriCryptographyException {
    List<RevValue> rev = get(null, index, revision);
    if (rev != null && rev.size() == 1) {
      return rev.get(0).getValue();
    } else {
      assert rev == null || rev.size() == 0 : "Rev size: " + rev.size();
      return null;
    }
  }

  /**
	 * Retrieve the value associated with {@code index} on the server.
	 * 
	 * @param index
	 * @return a byte array containing the data associated with {@code key} or {@code null} if no
	 * data exists.
	 */
	private List<RevValue> get(byte[] encKey, Index index, Revision revision) throws IOException, NigoriCryptographyException {

	  byte[] encIndex;
	  byte[] encRevision = null;
	  if (encKey == null) {
	    encIndex = keyManager.encryptDeterministically(index.getBytes());
	    if (revision != null) {
	      encRevision = keyManager.encryptDeterministically(revision.getBytes());
	    }
	  } else {
	    encIndex = keyManager.encryptDeterministically(encKey, index.getBytes());
	    if (revision != null) {
	      encRevision = keyManager.encryptDeterministically(encKey, revision.getBytes());
	    }
	  }

		Response response;
		try {
		  response = postResponse(MessageLibrary.REQUEST_GET,MessageLibrary.getRequestAsJson(keyManager.signer(), encIndex, encRevision));
		} catch (NoSuchAlgorithmException e) {
			throw new NigoriCryptographyException("Platform does have required crypto support:" +
					e.getMessage());
		}

		if (response.notFound()) {
			return null; //request was successful, but no data key by that name was found.
		}

		if (!success(response.resp)) {
			throw new IOException("Server did not accept request. " + response.jsonResponse);
		}

		try {
			GetResponse getResponse = MessageLibrary.getResponseFromJson(response.jsonResponse);
			List<RevisionValue> revisions = getResponse.getRevisionsList();
			List<RevValue> answer = new ArrayList<RevValue>(revisions.size());
      for (RevisionValue revisionValue : revisions) {
        byte[] revisionciphertext = revisionValue.getRevision().toByteArray();
        byte[] valueciphertext = revisionValue.getValue().toByteArray();
        if (encKey == null) {
          answer.add(new RevValue(keyManager.decrypt(revisionciphertext), keyManager
              .decrypt(valueciphertext)));
        } else {
          answer.add(new RevValue(keyManager.decrypt(encKey, revisionciphertext), keyManager
              .decrypt(encKey, valueciphertext)));
        }
      }
      return answer;
		} catch(JsonConversionException jce) {
			throw new IOException("Error reading JSON sent by server: "+ jce.getMessage());
		}
	}

	@Override
	public List<Revision> getRevisions(Index index) throws NigoriCryptographyException, UnsupportedEncodingException, IOException{
	  byte[] encIndex = keyManager.encryptDeterministically(index.getBytes());
	  Response response;
    try {
      response = postResponse(MessageLibrary.REQUEST_GET_REVISIONS, MessageLibrary.getRevisionsRequestAsJson(keyManager.signer(), encIndex));
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException("Platform does have required crypto support:" +
          e.getMessage());
    }

    if (response.notFound()) {
      return null; //request was successful, but no data key by that name was found.
    }

    if (!success(response.resp)) {
      throw new IOException("Server did not accept request. " + response.jsonResponse);
    }

    try {
      GetRevisionsResponse getResponse = MessageLibrary.getRevisionsResponseFromJson(response.jsonResponse);
      List<ByteString> revisions = getResponse.getRevisionsList();
      List<Revision> answer = new ArrayList<Revision>(revisions.size());
      for (ByteString revision : revisions) {
          answer.add(new Revision(keyManager.decrypt(revision.toByteArray())));
      }
      return answer;
    } catch(JsonConversionException jce) {
      throw new IOException("Error reading JSON sent by server: "+ jce.getMessage());
    }
	}

	@Override
  public boolean delete(Index index, byte[] token) throws UnsupportedEncodingException, NigoriCryptographyException, IOException {
    return delete(null, index, token);
  }

  private boolean delete(byte[] encKey, Index index, byte[] token) throws NigoriCryptographyException, UnsupportedEncodingException, IOException {
    byte[] encIndex;
    if (encKey == null) {
      encIndex = keyManager.encryptDeterministically(index.getBytes());
    } else {
      encIndex = keyManager.encryptDeterministically(encKey, index.getBytes());
    }
    Response response;

    try {
      response = postResponse(MessageLibrary.REQUEST_DELETE,MessageLibrary.deleteRequestAsJson(keyManager.signer(), encIndex));
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException("Platform does have required crypto support:" +
          e.getMessage());
    }

    if (response.notFound()) {
      return false; //request was successful, but no data key by that name was found.
    }

    if (!success(response.resp)) {
      throw new IOException("Server did not accept request("+ response.resp.getResponseCode() + "). " + response.jsonResponse);
    }
    return true;
  }

  @Override
  public List<Index> getIndices() throws NigoriCryptographyException, IOException {
    Response response;
    try {
      response = postResponse(MessageLibrary.REQUEST_GET_INDICES,MessageLibrary.getIndicesRequestAsJson(keyManager.signer()));
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException("Platform does have required crypto support:" +
          e.getMessage());
    }

    if (response.notFound()) {
      return null; //request was successful, but no data key by that name was found.
    }

    if (!success(response.resp)) {
      throw new IOException("Server did not accept request. " + response.jsonResponse);
    }

    try {
      GetIndicesResponse getResponse = MessageLibrary.getIndicesResponseFromJson(response.jsonResponse);
      List<ByteString> indices = getResponse.getIndicesList();
      List<Index> answer = new ArrayList<Index>(indices.size());
      for (ByteString index : indices) {
          answer.add(new Index(keyManager.decrypt(index.toByteArray())));
      }
      return answer;
    } catch(JsonConversionException jce) {
      throw new IOException("Error reading JSON sent by server: "+ jce.getMessage());
    }
  }

  private Response postResponse(String request, String jsonRequest)
      throws UnsupportedEncodingException, IOException {

    HttpResponse resp = post(request, jsonRequest.getBytes(MessageLibrary.CHARSET));

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