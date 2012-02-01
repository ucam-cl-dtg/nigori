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

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriMessages.GetRevisionsResponse;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.MessageLibrary.JsonConversionException;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.RevisionValue;
import com.google.protobuf.ByteString;

/**
 * A client API capable of managing a session with a Nigori Server.
 * 
 * @author Alastair Beresford
 *
 * It is worth looking at {@link java.util.Collection} but we can't implement that or most of the methods in it until we have a "list indexes" method which at least for now we don't intend to do.
 * putAll from {@link java.util.Map} might be worth implementing
 */
public class NigoriDatastore {

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
	public NigoriDatastore(String server, int port, String serverPrefix, String username,
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
	public NigoriDatastore(String server, int port, String serverPrefix) throws 
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

	/**
	 * Register the username and password details with the server.
	 * 
	 * @return true if the registration was successful; false otherwise.
	 */
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
	/**
   * Unregister the username and password details with the server.
   * 
   * @return true if the unregistration was successful; false otherwise.
   */
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

	/**
	 * Evaluate whether the current username and password represent a valid account on the server.
	 * 
	 * @return true if the account is valid; false otherwise.
	 */
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

	/**
	 * Insert a new key-value pair into the datastore of the server; only current user may read/write.
	 * 
	 * @param key the key
	 * @param value the data value associated with the key.
	 * @return true if the data was successfully inserted; false otherwise.
	 * @throws NigoriCryptographyException 
	 */
	public boolean put(byte[] index, byte[] revision, byte[] value)  throws IOException, NigoriCryptographyException {
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
	private boolean put(byte[] encKey, byte[] index, byte[] revision, byte[] value) throws IOException, 
	NigoriCryptographyException {

		byte[] encIndex;
		byte[] encRevision;
		byte[] encValue;
		if (encKey == null) {
			encIndex = keyManager.encryptDeterministically(index);
			encRevision = keyManager.encryptDeterministically(revision);
			encValue = keyManager.encrypt(value);
		} else {
			encIndex = keyManager.encryptDeterministically(encKey, index);
			encRevision = keyManager.encryptDeterministically(encKey, revision);
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

	/**
	 * Retrieve the value associated with {@code index} on the server.
	 * 
	 * @param index
	 * @return a byte array containing the data associated with {@code key} or {@code null} if no
	 * data exists.
	 */
	public List<RevValue> get(byte[] index) throws IOException,	NigoriCryptographyException {
		return get(null, index, null);
	}

	/**
   * @param index
   * @param revision
   * @return
	 * @throws NigoriCryptographyException 
	 * @throws IOException 
   */
  public byte[] getRevision(byte[] index, byte[] revision) throws IOException, NigoriCryptographyException {
    // TODO(drt24) Auto-generated method stub
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
	private List<RevValue> get(byte[] encKey, byte[] index, byte[] revision) throws IOException, NigoriCryptographyException {

	  byte[] encIndex;
	  byte[] encRevision = null;
	  if (encKey == null) {
	    encIndex = keyManager.encryptDeterministically(index);
	    if (revision != null) {
	      encRevision = keyManager.encryptDeterministically(revision);
	    }
	  } else {
	    encIndex = keyManager.encryptDeterministically(encKey, index);
	    if (revision != null) {
	      encRevision = keyManager.encryptDeterministically(encKey, revision);
	    }
	  }

		String jsonRequest;
		try {
			jsonRequest = MessageLibrary.getRequestAsJson(keyManager.signer(), encIndex, encRevision);
		} catch (NoSuchAlgorithmException e) {
			throw new NigoriCryptographyException("Platform does have required crypto support:" +
					e.getMessage());
		}

		HttpResponse resp = post(MessageLibrary.REQUEST_GET, 
				jsonRequest.getBytes(MessageLibrary.CHARSET));

		BufferedInputStream in = new BufferedInputStream(resp.getInputStream());
		StringBuilder jsonResponse = new StringBuilder();
		//TODO(beresford): optimise this buffer size
		byte[] buffer = new byte[1024];
		int bytesRead;
		while((bytesRead = in.read(buffer)) != -1) {
			jsonResponse.append(new String(buffer, 0, bytesRead, MessageLibrary.CHARSET));
		}

		if (resp.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
			return null; //request was successful, but no data key by that name was found.
		}

		success(resp);
		if (resp.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new IOException("Server did not accept request. " + jsonResponse);
		}

		try {
			GetResponse getResponse = MessageLibrary.getResponseFromJson(jsonResponse.toString());
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

	public List<byte[]> getRevisions(byte[] index) throws NigoriCryptographyException, UnsupportedEncodingException, IOException{
	  byte[] encIndex = keyManager.encryptDeterministically(index);
	  String jsonRequest;
    try {
      jsonRequest = MessageLibrary.getRevisionsAsJson(keyManager.signer(), encIndex);
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException("Platform does have required crypto support:" +
          e.getMessage());
    }
    
    HttpResponse resp = post(MessageLibrary.REQUEST_GET_REVISIONS, 
        jsonRequest.getBytes(MessageLibrary.CHARSET));
    
    BufferedInputStream in = new BufferedInputStream(resp.getInputStream());
    StringBuilder jsonResponse = new StringBuilder();
    //TODO(beresford): optimise this buffer size
    byte[] buffer = new byte[1024];
    int bytesRead;
    while((bytesRead = in.read(buffer)) != -1) {
      jsonResponse.append(new String(buffer, 0, bytesRead, MessageLibrary.CHARSET));
    }

    if (resp.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
      return null; //request was successful, but no data key by that name was found.
    }

    success(resp);
    if (resp.getResponseCode() != HttpURLConnection.HTTP_OK) {
      throw new IOException("Server did not accept request. " + jsonResponse);
    }

    try {
      GetRevisionsResponse getResponse = MessageLibrary.getRevisionsResponseFromJson(jsonResponse.toString());
      List<ByteString> revisions = getResponse.getRevisionsList();
      List<byte[]> answer = new ArrayList<byte[]>(revisions.size());
      for (ByteString revision : revisions) {
          answer.add(keyManager.decrypt(revision.toByteArray()));
      }
      return answer;
    } catch(JsonConversionException jce) {
      throw new IOException("Error reading JSON sent by server: "+ jce.getMessage());
    }
	}
	/**
	 * Delete the key (and associated value) on the server
	 * @param key
	 * @return true if the deletion was successful; false if no such key was found or a server error
	 * occurred.
	 * @throws IOException 
	 * @throws NigoriCryptographyException 
	 * @throws UnsupportedEncodingException 
	 */
  public boolean delete(byte[] index) throws UnsupportedEncodingException, NigoriCryptographyException, IOException {
    return delete(null, index);
  }

  private boolean delete(byte[] encKey, byte[] index) throws NigoriCryptographyException, UnsupportedEncodingException, IOException {
    byte[] encIndex;
    if (encKey == null) {
      encIndex = keyManager.encryptDeterministically(index);
    } else {
      encIndex = keyManager.encryptDeterministically(encKey, index);
    }

    String jsonRequest;
    try {
      jsonRequest = MessageLibrary.deleteRequestAsJson(keyManager.signer(), encIndex);
    } catch (NoSuchAlgorithmException e) {
      throw new NigoriCryptographyException("Platform does have required crypto support:" +
          e.getMessage());
    }

    HttpResponse resp = post(MessageLibrary.REQUEST_DELETE,
        jsonRequest.getBytes(MessageLibrary.CHARSET));

    BufferedInputStream in = new BufferedInputStream(resp.getInputStream());
    StringBuilder jsonResponse = new StringBuilder();
    //TODO(beresford): optimise this buffer size
    byte[] buffer = new byte[1024];
    int bytesRead;
    while((bytesRead = in.read(buffer)) != -1) {
      jsonResponse.append(new String(buffer, 0, bytesRead, MessageLibrary.CHARSET));
    }

    if (resp.getResponseCode() ==  HttpURLConnection.HTTP_NOT_FOUND) {
      return false; //request was successful, but no data key by that name was found.
    }

    success(resp);
    if (resp.getResponseCode() !=  HttpURLConnection.HTTP_OK) {
      throw new IOException("Server did not accept request("+ resp.getResponseCode() + "). " + jsonResponse);
    }
    return true;
  }
}