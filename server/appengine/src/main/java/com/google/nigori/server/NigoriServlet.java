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

import static com.google.nigori.common.MessageLibrary.toBytes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.MessageLibrary.JsonConversionException;
import com.google.nigori.common.NigoriMessages.AuthenticateRequest;
import com.google.nigori.common.NigoriMessages.DeleteRequest;
import com.google.nigori.common.NigoriMessages.GetIndicesRequest;
import com.google.nigori.common.NigoriMessages.GetRequest;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.GetRevisionsRequest;
import com.google.nigori.common.NigoriMessages.PutRequest;
import com.google.nigori.common.NigoriMessages.RegisterRequest;
import com.google.nigori.common.NigoriMessages.UnregisterRequest;
import com.google.nigori.common.NigoriProtocol;
import com.google.nigori.common.NotFoundException;
import com.google.nigori.common.UnauthorisedException;
import com.google.nigori.server.appengine.AppEngineDatabase;

public class NigoriServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final boolean DEBUG_JSON = false;
  private static final Logger log = Logger.getLogger(NigoriServlet.class.getName());
	private static final int maxJsonQueryLength = 1024*1024*1;
	private final NigoriProtocol protocol;

	public NigoriServlet() {
		this(new AppEngineDatabase());
	}

	public NigoriServlet(Database database) {
		super();
		this.protocol = new DatabaseNigoriProtocol(database);
	}

	private class ServletException extends Exception {
    private static final long serialVersionUID = 1L;
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
			resp.getOutputStream().write(toBytes(this.getMessage()));
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
					throw new ServletException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Json request exceeds server maximum length of " + 
							maxLength);
				}
				json.append(buffer, 0, charsRead);
			}
			return json.toString();
		} catch (IOException ioe) {
			throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error receiving data from client.");
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
		public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, JsonConversionException, UnauthorisedException, NotFoundException;
	}

  private class JsonGetRequestHandler implements RequestHandler {

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException, JsonConversionException, NotFoundException, UnauthorisedException {

      String json = getJsonAsString(req, maxJsonQueryLength);
      GetRequest request = MessageLibrary.getRequestFromJson(json);
      GetResponse response = protocol.get(request);

      String jsonresponse = MessageLibrary.toJson(response);
      resp.setContentType(MessageLibrary.MIMETYPE_JSON);
      resp.setCharacterEncoding(MessageLibrary.CHARSET);
      resp.setStatus(HttpServletResponse.SC_OK);
      BufferedWriter w = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream()));
      w.write(jsonresponse);
      w.flush();
    }
  }

  private class JsonGetIndicesRequestHandler implements RequestHandler {

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException, NotFoundException, UnauthorisedException, JsonConversionException {

      String json = getJsonAsString(req, maxJsonQueryLength);

      GetIndicesRequest request = MessageLibrary.getIndicesRequestFromJson(json);

      String response = MessageLibrary.toJson(protocol.getIndices(request));
      resp.setContentType(MessageLibrary.MIMETYPE_JSON);
      resp.setCharacterEncoding(MessageLibrary.CHARSET);
      resp.setStatus(HttpServletResponse.SC_OK);
      BufferedWriter w = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream()));
      w.write(response);
      w.flush();
    }
  }

	private class JsonGetRevisionsRequestHandler implements RequestHandler {

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException, NotFoundException, UnauthorisedException, JsonConversionException {

      String json = getJsonAsString(req, maxJsonQueryLength);

      GetRevisionsRequest request = MessageLibrary.getRevisionsRequestFromJson(json);

      String response = MessageLibrary.toJson(protocol.getRevisions(request));
      resp.setContentType(MessageLibrary.MIMETYPE_JSON);
      resp.setCharacterEncoding(MessageLibrary.CHARSET);
      resp.setStatus(HttpServletResponse.SC_OK);
      BufferedWriter w = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream()));
      w.write(response);
      w.flush();
    }
  }

	private class JsonPutRequestHandler implements RequestHandler {

	  @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp) 
	      throws ServletException, JsonConversionException, IOException, UnauthorisedException {
	    String json = getJsonAsString(req, maxJsonQueryLength);
	    if (DEBUG_JSON) {
	      System.out.println(json);
	    }
	    PutRequest request = MessageLibrary.putRequestFromJson(json);

	    if (!protocol.put(request)) {
	      throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
	          "Internal storage error for key " + Base64.encodeBase64String(request.getKey().toByteArray()));
	    }

	    emptyBody(resp);
	  }
	}

	private class JsonDeleteRequestHandler implements RequestHandler {

	  @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp)
	      throws ServletException, JsonConversionException, IOException, UnauthorisedException, NotFoundException {
	    String json = getJsonAsString(req, maxJsonQueryLength);

	    if (DEBUG_JSON) {
	      System.out.println(json);
	    }
	    DeleteRequest request = MessageLibrary.deleteRequestFromJson(json);


	    if (!protocol.delete(request)) {
	      throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
	          "Internal storage error for key " + Base64.encodeBase64String(request.getKey().toByteArray()));
	    }

	    emptyBody(resp);
	  }
	}

	private class JsonAuthenticateRequestHandler implements RequestHandler {

	  @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp)
	      throws ServletException, JsonConversionException, IOException, UnauthorisedException {
	    String json = getJsonAsString(req, maxJsonQueryLength);
	    AuthenticateRequest auth = MessageLibrary.authenticateRequestFromJson(json);
	    boolean success = protocol.authenticate(auth);
	    if (!success){
	      throw new UnauthorisedException("Authorisation failed");
	    }

	    emptyBody(resp);
	  }
	}

	private class JsonRegisterRequestHandler implements RequestHandler {

	  @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp)
	      throws ServletException, JsonConversionException, IOException {

	    String json = getJsonAsString(req, maxJsonQueryLength);
	    RegisterRequest request = MessageLibrary.registerRequestFromJson(json);

	    boolean success = protocol.register(request);
	    if(!success) {
	      throw new ServletException(HttpServletResponse.SC_CONFLICT,
	          "Adding user " + Base64.encodeBase64String(request.getPublicKey().toByteArray()) + " failed, may already exist");
	    }
	    emptyBody(resp);
	  }
	}

	private class JsonUnregisterRequestHandler implements RequestHandler {

	  @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp)
	      throws ServletException, IOException, UnauthorisedException, JsonConversionException {
	    String json = getJsonAsString(req, maxJsonQueryLength);
	    UnregisterRequest request = MessageLibrary.unregisterRequestFromJson(json);

	    boolean success = protocol.unregister(request);
	    if(!success) {
	      throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
	          "Removing user " + Base64.encodeBase64String(request.getAuth().getPublicKey().toByteArray()) + " failed");
	    }
	    emptyBody(resp);
	  }
	}

	//TODO(beresford): double-check that Servlet instances are created rarely
	private String supportedTypes = null;
	private HashMap<RequestHandlerType, RequestHandler> handlers = initHandlers();
	private HashMap<RequestHandlerType, RequestHandler> initHandlers() {
		HashMap<RequestHandlerType, RequestHandler> h = 
			new HashMap<RequestHandlerType, RequestHandler>();
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_GET), 
				new JsonGetRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_GET_INDICES), 
        new JsonGetIndicesRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_GET_REVISIONS), 
        new JsonGetRevisionsRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_PUT), 
				new JsonPutRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_DELETE), 
        new JsonDeleteRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_AUTHENTICATE),
				new JsonAuthenticateRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_REGISTER),
				new JsonRegisterRequestHandler());
		h.put(new RequestHandlerType(MessageLibrary.MIMETYPE_JSON, MessageLibrary.REQUEST_UNREGISTER),
		    new JsonUnregisterRequestHandler());
		StringBuilder supportedPairs = new StringBuilder("The following mimetypes and request pairs are supported: ");
		for (RequestHandlerType type : h.keySet()){
		  supportedPairs.append("(" + type.mimetype + " - " + type.requestType + ") ");
		}
		supportedTypes = supportedPairs.toString();
		return h;
	}

	/**
	 * Handle initial request from client and dispatch to appropriate handler or return error message.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

	  try {
      // Enable cors: http://enable-cors.org/server.html to allow access from javascript/dart
      // clients using code from a different domain
      resp.addHeader("Access-Control-Allow-Origin", "*");
      resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      resp.addHeader("Access-Control-Allow-Headers",
          "Origin, X-Requested-With, Content-Type, Accept");
	    //Subset of path managed by this servlet; e.g. if URI is "/nigori/get" and servlet path
	    //is "/nigori, then we want to retrieve "get" as the request type
	    int startIndex = req.getServletPath().length() + 1;
	    String requestURI = req.getRequestURI();
      if (requestURI.length() <= startIndex) {
        ServletException s =
            new ServletException(HttpServletResponse.SC_BAD_REQUEST, "No request type specified.\n"
                + supportedTypes + "\n");
        log.fine(s.toString());
        s.writeHttpResponse(resp);
        return;
      }
	    String requestType = requestURI.substring(startIndex);
	    String requestMimetype = req.getContentType();
	    RequestHandlerType handlerType = new RequestHandlerType(requestMimetype, requestType);

	    RequestHandler handler = handlers.get(handlerType);
	    if (handler == null) {
	      throw new ServletException(HttpServletResponse.SC_NOT_ACCEPTABLE,
	          "Unsupported request pair: " + handlerType + "\n" + supportedTypes);
	    }
	    try {
	      handler.handle(req, resp);
	    } catch (NotFoundException e) {
	      ServletException s =  new ServletException(HttpServletResponse.SC_NOT_FOUND, e.getLocalizedMessage());
	      log.fine(s.toString());
	      s.writeHttpResponse(resp);
	    } catch (UnauthorisedException e) {
	      ServletException s = new ServletException(HttpServletResponse.SC_UNAUTHORIZED, "Authorisation failed: "
	          + e.getLocalizedMessage());
	      log.warning(s.toString());
	      s.writeHttpResponse(resp);
	    } catch (IOException ioe) {
	      throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
	          "Internal error sending data to client");
	    } catch (MessageLibrary.JsonConversionException jce) {
	      throw new ServletException(HttpServletResponse.SC_BAD_REQUEST, "JSON format error: "
	          + jce.getMessage());
	    } catch (RuntimeException re) {
	      log.severe(re.toString());
	      throw new ServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, re.toString());
	    }

	  } catch (ServletException e) {
	    log.severe(e.toString());
	    e.writeHttpResponse(resp);
	  }
	}
}