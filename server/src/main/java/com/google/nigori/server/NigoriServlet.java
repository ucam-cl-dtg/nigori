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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriMessages.AuthenticateRequest;
import com.google.nigori.common.NigoriMessages.GetRequest;
import com.google.nigori.common.NigoriMessages.PutRequest;
import com.google.nigori.common.NigoriMessages.RegisterRequest;
import com.google.nigori.common.Nonce;
import com.google.nigori.common.SchnorrSignature;
import com.google.nigori.common.SchnorrVerify;

@SuppressWarnings("serial")
public class NigoriServlet extends HttpServlet {

  private static final Logger log = Logger.getLogger(NigoriServlet.class.getName());
	private static final int maxJsonQueryLength = 1024*1024*1;
	private final Database database;

	public NigoriServlet() {
		super();
		this.database = new TestDatabase();
	}

	public NigoriServlet(Database database) {
		super();
		this.database = database;
	}

	private class ServletException extends Exception {
		private int statusCode;

		ServletException(int statusCode, String message) {
			super(message);
			this.statusCode = statusCode;
		}

		int getStatusCode() {
			return statusCode;
		}

		void writeHttpResponse(HttpServletResponse resp) throws IOException {
			resp.setContentType(MessageLibrary.MIMETYPE_JSON);
			resp.setCharacterEncoding(MessageLibrary.CHARSET);
			resp.setStatus(getStatusCode());
			resp.getOutputStream().write(this.getMessage().getBytes(MessageLibrary.CHARSET));		
		}			
	}

	private String getJsonAsString(HttpServletRequest req, int maxLength) 
	throws ServletException {

		if (maxLength != 0 && req.getContentLength() > maxLength) {
			return null;
		}

		String charsetName = req.getCharacterEncoding();
		if (charsetName == null) {
			charsetName = MessageLibrary.CHARSET;
		}

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(req.getInputStream(),
					charsetName));
			StringBuilder json = new StringBuilder();
			char[] buffer = new char[64 * 1024];
			int charsRemaining = maxJsonQueryLength;
			int charsRead;
			while((charsRead = in.read(buffer)) != -1) {
				charsRemaining -= charsRead;
				if (charsRemaining < 0) {
					throw new ServletException(413, "Json request exceeds server maximum length of " + 
							maxLength);
				}
				json.append(buffer, 0, charsRead);
			}
			return json.toString();
		} catch (IOException ioe) {
			throw new ServletException(500, "Internal error receiving data from client.");
		}
	}

	/**
	 * Class to support efficient lookup of appropriate handler method for a particular request.
	 */
	private class RequestHandlerType {

		private String mimetype;
		private String requestType;

		RequestHandlerType(String mimetype, String requestType) {
			this.mimetype = mimetype;
			this.requestType = requestType;
		}

		@Override
		public int hashCode() {
			return mimetype.hashCode() + requestType.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RequestHandlerType) {
				RequestHandlerType r = (RequestHandlerType) obj;
				return this.hashCode() == r.hashCode();
			}
			return false;
		}

		@Override
		public String toString() {
			return "<" + mimetype + "," + requestType + "> (hashCode:" + hashCode() + ")";
		}
	}

	/**
	 * Check {@code nonce} is valid. If so, this method returns; else throws a ServletException
	 * 
	 * @param schnorrS
	 * @param schnorrE
	 * @param message
	 * @throws ServletException
	 */
  private User authenticateUser(AuthenticateRequest auth)
      throws ServletException {

    byte[] publicKey = auth.getPublicKey().toByteArray();
    byte[] schnorrE = auth.getSchnorrE().toByteArray();
    byte[] schnorrS = auth.getSchnorrS().toByteArray();
    byte[] nonce = auth.getNonce().toByteArray();

    SchnorrSignature sig = new SchnorrSignature(schnorrE, schnorrS, nonce);
    try {
      SchnorrVerify v = new SchnorrVerify(publicKey);
      Nonce n = new Nonce(sig.getMessage());
      // TODO(beresford): Put this constant in server configuration and use this to timeout storage
      // of random nonce values out of the database (when we start to store them!).
      boolean timestampRecent =
          n.getSinceEpoch() - System.currentTimeMillis() / 1000 < 60 * 60 * 24 * 2;
      // TODO(beresford): Must check that n.getRandom() has not be used before.
      // Must avoid race condition when random number is used multiple times quickly
      boolean nonceUnique = true;
      boolean userExists = database.haveUser(publicKey);

      try {
        if (v.verify(sig) && timestampRecent && nonceUnique && userExists) {
          return database.getUser(publicKey);
        }
      } catch (NoSuchAlgorithmException nsae) {
        throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Internal error attempting to verify signature");
      }
    } catch (UserNotFoundException e) {
      // TODO(drt24): potential security vulnerability - user existence oracle.
      throw new ServletException(HttpServletResponse.SC_NOT_FOUND, "No such user");
    }
    throw new ServletException(HttpServletResponse.SC_UNAUTHORIZED, "The signature is invalid");
  }

	/**
	 * Send an SC_OK and and empty body
	 * @param resp
	 * @throws ServletException
	 */
	private void emptyBody(HttpServletResponse resp) throws 
	ServletException {
		try {
			resp.setContentType(MessageLibrary.MIMETYPE_JSON);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.flushBuffer();
		} catch (IOException ioe) {
			throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
			"Error attempting to write status OK message after successfully handling a PutRequest");
		}
	}

	private abstract interface RequestHandler {
		public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException;
	}

	private class JsonGetRequestHandler implements RequestHandler {

		public void handle(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException {

			String json = getJsonAsString(req, maxJsonQueryLength);
			byte[] value;

			try {
				GetRequest request = MessageLibrary.getRequestFromJson(json);
				byte[] key = request.getKey().toByteArray();
				AuthenticateRequest auth = request.getAuth();
				User user = authenticateUser(auth);

				value = database.getRecord(user, key);

				if (value == null) {
					throw new ServletException(HttpServletResponse.SC_NOT_FOUND, "Cannot find requested key");
				}

				String response = MessageLibrary.getResponseAsJson(value);
				resp.setContentType(MessageLibrary.MIMETYPE_JSON);
				resp.setCharacterEncoding(MessageLibrary.CHARSET);
				resp.setStatus(HttpServletResponse.SC_OK);
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream()));
				w.write(response);
				w.flush();
			} catch(IOException ioe) {
				throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error sending data to client");
			} catch (MessageLibrary.JsonConversionException jce) {
				throw new ServletException(HttpServletResponse.SC_BAD_REQUEST, "JSON format error: " + 
						jce.getMessage());
			}
		}
	}

	private class JsonPutRequestHandler implements RequestHandler {

		public void handle(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException {

			try {
				String json = getJsonAsString(req, maxJsonQueryLength);
				
				System.out.println(json);
				PutRequest request = MessageLibrary.putRequestFromJson(json);
				AuthenticateRequest auth = request.getAuth();
				
				User user = authenticateUser(auth);

				boolean success = database.putRecord(user, request.getKey().toByteArray(), 
						request.getValue().toByteArray());

				if (!success) {
					throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
							"Internal storage error for key " + Hex.encodeHexString(request.getKey().toByteArray()));
				}

				emptyBody(resp);

			} catch (MessageLibrary.JsonConversionException jce) {
				throw new ServletException(HttpServletResponse.SC_BAD_REQUEST, "JSON format error: " + 
						jce.getMessage());
			}

		}
	}

	private class JsonAuthenticateRequestHandler implements RequestHandler {

		public void handle(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException {

			try {
				String json = getJsonAsString(req, maxJsonQueryLength);
				AuthenticateRequest auth = MessageLibrary.authenticateRequestFromJson(json);
				authenticateUser(auth);

				emptyBody(resp);

			} catch (MessageLibrary.JsonConversionException jce) {
				throw new ServletException(HttpServletResponse.SC_BAD_REQUEST, "JSON format error: " + 
						jce.getMessage());
			}

		}
	}

	private class JsonRegisterRequestHandler implements RequestHandler {

		public void handle(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException {

			try {
				String json = getJsonAsString(req, maxJsonQueryLength);
				RegisterRequest request = MessageLibrary.registerRequestFromJson(json);

				boolean success = database.addUser(request.getPublicKey().toByteArray());
				if(!success) {
					throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Adding user " + Hex.encodeHexString(request.getPublicKey().toByteArray()) + " failed");
				}
				emptyBody(resp);

			} catch (MessageLibrary.JsonConversionException jce) {
				throw new ServletException(HttpServletResponse.SC_BAD_REQUEST, "JSON format error: " + 
						jce.getMessage());
			}
		}
	}

	//TODO(beresford): double-check that Servlet instances are created rarely
	private HashMap<RequestHandlerType, RequestHandler> handlers = initHandlers();
	private HashMap<RequestHandlerType, RequestHandler> initHandlers() {
		HashMap<RequestHandlerType, RequestHandler> h = 
			new HashMap<RequestHandlerType, RequestHandler>();
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_GET), 
				new JsonGetRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_PUT), 
				new JsonPutRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_AUTHENTICATE),
				new JsonAuthenticateRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_REGISTER),
				new JsonRegisterRequestHandler());
		return h;
	}

	/**
	 * Handle initial request from client and dispatch to appropriate handler or return error message.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		try {
			//Subset of path managed by this servlet; e.g. if URI is "/nigori/get" and servlet path
			//is "/nigori, then we want to retrieve "get" as the request type
			String requestType = req.getRequestURI().substring(req.getServletPath().length() + 1);
			String requestMimetype = req.getContentType();
			RequestHandlerType handlerType = new RequestHandlerType(requestMimetype, requestType);

			RequestHandler handler = handlers.get(handlerType);
			if (handler == null) {
				throw new ServletException(HttpServletResponse.SC_BAD_REQUEST, 
						"Unsupported request pair:" + handlerType);
			}			
			handler.handle(req, resp);

		} catch (ServletException e) {
		  log.severe(e.toString());
			e.writeHttpResponse(resp);
		}
	}
}