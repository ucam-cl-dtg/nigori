/*
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
package com.google.nigori.server;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.nigori.client.KeyManager;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriMessages.GetRequest;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.protobuf.Descriptors.FieldDescriptor;

public class NigoriServletTest {

	private static LocalServiceTestHelper helper;
  Database database;
	NigoriServlet servlet;
	HttpServletRequest request;
	HttpServletResponse response;
	KeyManager keyManager;
	User user;
	
	@BeforeClass
	public static void init(){
	  helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
	}
	@Before
	public void setUp() throws Exception {
		helper.setUp();
		database = createMock(Database.class);
		request = createMock(HttpServletRequest.class);
		response = createMock(HttpServletResponse.class);
		servlet = new NigoriServlet(database);
		keyManager = new KeyManager("localhost:8888".getBytes(MessageLibrary.CHARSET),
				"username".getBytes(MessageLibrary.CHARSET),
				"password".getBytes(MessageLibrary.CHARSET));
		//TODO need to correctly create user
		user = new User(keyManager.getUsername(), keyManager.signer().getPublicKey(), new Date());
	}

	@After
	public void tearDown() {
	  helper.tearDown();
	}
	
	class TestInputStream extends ServletInputStream {
		ByteArrayInputStream in;
		TestInputStream(String s) throws UnsupportedEncodingException {
			
			in = new ByteArrayInputStream(s.getBytes(MessageLibrary.CHARSET));
		}
		@Override
		public int read() throws IOException {
			
			return in.read();
		}
	}

	private void expectedCallsForJsonRequest(String json, String requestType) throws IOException {
		
		expect(request.getServletPath()).andReturn("nigori");
		expect(request.getRequestURI()).andReturn("nigori/" + requestType);
		expect(request.getContentType()).andReturn(MessageLibrary.MIMETYPE_JSON);
		expect(request.getContentLength()).andReturn(json.length());
		expect(request.getCharacterEncoding()).andReturn(MessageLibrary.CHARSET);
		expect(request.getInputStream()).andReturn(new TestInputStream(json));		
	}
	
	private ServletOutputStream expectedCallsForResponse(int statusCode, String mimetype) throws 
	IOException {
		
		final ServletOutputStream out = createMock(ServletOutputStream.class);
		response.setContentType(mimetype);
		response.setCharacterEncoding(MessageLibrary.CHARSET);
		response.setStatus(statusCode);
		expect(response.getOutputStream()).andReturn(out);
		return out;
	}
	
	private ServletOutputStream expectedCallsForJsonResponse() throws IOException {
		
		return expectedCallsForResponse(HttpServletResponse.SC_OK, MessageLibrary.MIMETYPE_JSON);
	}
	
	private ServletOutputStream expectedCallsForErrorResponse(int statusCode) throws IOException {
		
		ServletOutputStream out = expectedCallsForResponse(statusCode, MessageLibrary.MIMETYPE_JSON);
		out.write((byte[]) notNull());		
		return out;
	}
	
	private void expectedCallsToOutputOkay() throws IOException {
		
		response.setContentType(MessageLibrary.MIMETYPE_JSON);
		response.setStatus(HttpServletResponse.SC_OK);
		response.flushBuffer();
	}

	private void runReplayVerifyWithDoPost(ServletOutputStream out) throws IOException {
		
		replay(request);
		replay(database);
		replay(response);
		if (out != null) {
			replay(out);
		}
		
		servlet.doPost(request, response);
		
		verify(request);
		verify(database);
		verify(response);
		if (out != null) {
			verify(out);
		}
	}
	
	@Test
	public void testGetRequestKeyDoesNotExist() throws Exception {

		final byte[] key = "a key".getBytes(MessageLibrary.CHARSET);
		final byte[] authority = keyManager.signer().getPublicKey();
		
		final String json = MessageLibrary.getRequestAsJson(keyManager.signer(), key);		
		expectedCallsForJsonRequest(json, MessageLibrary.REQUEST_GET);
		expect(database.haveUser(aryEq(authority))).andReturn(true);
		expect(database.getRecord(eq(user), aryEq(key))).andReturn(null);
		ServletOutputStream out = expectedCallsForErrorResponse(HttpServletResponse.SC_NOT_FOUND);

		runReplayVerifyWithDoPost(out);
	}
	
	@Test
	public void testPutRequest() throws Exception {
		
		final byte[] key = "a key".getBytes(MessageLibrary.CHARSET);
		final byte[] value = "a value".getBytes(MessageLibrary.CHARSET);
		final byte[] authority = keyManager.signer().getPublicKey();
		
		final String jsonPut = MessageLibrary.putRequestAsJson(keyManager.signer(), key, value);		
		expectedCallsForJsonRequest(jsonPut, MessageLibrary.REQUEST_PUT);
		expect(database.haveUser(aryEq(authority))).andReturn(true);
		expect(database.putRecord(eq(user), aryEq(key), aryEq(value))).andReturn(true);
		expectedCallsToOutputOkay();
		
		runReplayVerifyWithDoPost(null);
	}
	
	@Test
	public void testGetRequestKeyExists() throws Exception {

		final byte[] key = "a key".getBytes(MessageLibrary.CHARSET);
		final byte[] value = "a value".getBytes(MessageLibrary.CHARSET);
		final byte[] authority = keyManager.signer().getPublicKey();

		final String jsonGet = MessageLibrary.getRequestAsJson(keyManager.signer(), key);		
		expectedCallsForJsonRequest(jsonGet, MessageLibrary.REQUEST_GET);
		expect(database.haveUser(aryEq(authority))).andReturn(true);
		expect(database.getRecord(eq(user), aryEq(key))).andReturn(value);
		ServletOutputStream out = expectedCallsForJsonResponse();
		Capture<byte[]> result = new Capture<byte[]>();
		Capture<Integer> size = new Capture<Integer>();
		out.write(capture(result), eq(0), capture(size));
		out.flush();
		
		runReplayVerifyWithDoPost(out);
		
		String jsonResponse = new String(result.getValue(), 0, size.getValue(), MessageLibrary.CHARSET);
		GetResponse response = MessageLibrary.getResponseFromJson(jsonResponse);
		assertTrue(Arrays.equals(response.getValue().toByteArray(), value));
	}
	
	@Test
	public void testMalformattedGetRequest() throws Exception {
		
		final String jsonGet = "A malformed JSON message";
		expectedCallsForJsonRequest(jsonGet, MessageLibrary.REQUEST_GET);
		ServletOutputStream out = expectedCallsForErrorResponse(HttpServletResponse.SC_BAD_REQUEST);

		runReplayVerifyWithDoPost(out);
	}

	@Test
	public void testValidJSONGetRequestWithoutAppropriateFields() throws Exception {
		
		final String jsonGet = "{}";
		expectedCallsForJsonRequest(jsonGet, MessageLibrary.REQUEST_GET);
		ServletOutputStream out = expectedCallsForErrorResponse(HttpServletResponse.SC_BAD_REQUEST);

		runReplayVerifyWithDoPost(out);
	}

	@Test
	public void testJSONGetRequestWithCorrectFieldsButCorruptedValues() throws Exception {

		//Build a broken version of the JSON request which has valid keys but invalid values
		byte[] key = "a key".getBytes(MessageLibrary.CHARSET);
		GetRequest get = MessageLibrary.getRequestAsProtobuf(keyManager.signer(), key);		
		Map<FieldDescriptor, Object> fieldMap = get.getAllFields();
		
		StringBuilder json = new StringBuilder();
		String invalidValue = "not a base64 encoded value due to incorrect symbols: ~`'|";
		json.append('{');
		for(FieldDescriptor f : fieldMap.keySet()) {
			json.append("\"" + f.getName() + "\": \"" + invalidValue + "\",");
		}
		json.deleteCharAt(json.length() - 1);
		json.append('}');
		String jsonGet = json.toString();
		
		expectedCallsForJsonRequest(jsonGet, MessageLibrary.REQUEST_GET);
		ServletOutputStream out = expectedCallsForErrorResponse(HttpServletResponse.SC_BAD_REQUEST);

		runReplayVerifyWithDoPost(out);
	}
	
	//TODO(beresford): Tests similar to Get above for Put, Authenticate, and Register
}