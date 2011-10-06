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

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.MessageLibrary.JsonConversionException;
import com.google.nigori.common.NigoriMessages.GetResponse;

/**
 * A client API capable of managing a session with a Nigori Server.
 * 
 * @author Alastair Beresford
 *
 */
public class NigoriDatastore {

	private final String serverUrl;
	private final KeyManager keyManager;

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
		keyManager = new KeyManager(servername.getBytes(MessageLibrary.CHARSET),
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

		keyManager = new KeyManager(servername.getBytes(MessageLibrary.CHARSET));
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
			String json = MessageLibrary.registerRequestAsJson(keyManager.signer());
			HttpResponse resp = post(MessageLibrary.REQUEST_REGISTER,
					json.getBytes(MessageLibrary.CHARSET));
			return resp.getResponseCode() == 200;
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
			return resp.getResponseCode() == 200;
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
	public boolean put(byte[] index, byte[] value)  throws IOException, NigoriCryptographyException {
		return put(null, index, value, getPublicKey());
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
	private boolean put(byte[] encKey, byte[] index, byte[] value, byte[] reader) throws IOException, 
	NigoriCryptographyException {

		byte[] encIndex;
		byte[] encValue;
		if (encKey == null) {
			encIndex = keyManager.encryptWithZeroIv(index);
			encValue = keyManager.encrypt(value);
		} else {
			encIndex = keyManager.encryptWithZeroIv(encKey, index);
			encValue = keyManager.encrypt(encKey, value);
		}

		byte[][] readers = new byte[][]{reader};
		byte[][] writers = new byte[0][];

		try {
			String json = MessageLibrary.putRequestAsJson(keyManager.signer(), encIndex, encValue, readers,
					writers);
			HttpResponse resp = post(MessageLibrary.REQUEST_PUT, json.getBytes(MessageLibrary.CHARSET));
			return resp.getResponseCode() == 200;
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
	public byte[] get(byte[] index) throws IOException,	NigoriCryptographyException {
		return get(null, index);
	}

	/**
	 * Retrieve the value associated with {@code index} on the server.
	 * 
	 * @param index
	 * @return a byte array containing the data associated with {@code key} or {@code null} if no
	 * data exists.
	 */
	private byte[] get(byte[] encKey, byte[] index) throws IOException, NigoriCryptographyException {

		byte[] encIndex;
		if (encKey == null) {
			encIndex = keyManager.encryptWithZeroIv(index);
		} else {
			encIndex = keyManager.encryptWithZeroIv(encKey, index);
		}

		String jsonRequest;
		try {
			jsonRequest = MessageLibrary.getRequestAsJson(keyManager.signer(), encIndex);
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

		if (resp.getResponseCode() == 404) {
			return null; //request was successful, but no data key by that name was found.
		}

		if (resp.getResponseCode() != 200) {
			throw new IOException("Server did not accept request. " + jsonResponse);
		}

		try {
			GetResponse getResponse = MessageLibrary.getResponseFromJson(jsonResponse.toString());
			byte[] ciphertext = getResponse.getValue().toByteArray();
			if (encKey == null) {
				return keyManager.decrypt(ciphertext);
			} else {
				return keyManager.decrypt(encKey, ciphertext);
			}
		} catch(JsonConversionException jce) {
			throw new IOException("Error reading JSON sent by server: "+ jce.getMessage());
		}
	}

	/**
	 * Delete the key (and associated value) on the server
	 * @param key
	 * @return true if the deletion was successful; false if no such key was found or a server error
	 * occurred.
	 */
	public boolean delete(byte[] key) {
		//TODO(beresford): complete this API call
		return false;
	}

	/**
	 * Creates a new shared key suitable for use with the client-side encryption
	 */
	public byte[] generateSessionKey() {
		return keyManager.generateSessionKey();
	}
}