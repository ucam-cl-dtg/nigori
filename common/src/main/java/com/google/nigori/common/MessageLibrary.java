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
package com.google.nigori.common;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
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
import com.google.nigori.common.NigoriMessages.RevisionValue;
import com.google.nigori.common.NigoriMessages.UnregisterRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

public class MessageLibrary {

	//The character set used to encode all string-based communications in Nigori.
	public static final String CHARSET = "UTF-8";

  public static byte[] toBytes(String string) {
    try {
      return string.getBytes(CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);// never happens as UTF-8 is supported
    }
  }
  public static String bytesToString(byte[] bytes){
    try {
      return new String(bytes,CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);// never happens as UTF-8 is supported
    }
  }

	//The mimetypes used for all supported communication formats in Nigori
	public static final String MIMETYPE_JSON = "application/json";
	public static final String MIMETYPE_PROTOBUF = "application/x-google-protobuf";

	public static final String REQUEST_GET = "get";
	public static final String REQUEST_GET_INDICES = "get-indices";
	public static final String REQUEST_GET_REVISIONS = "get-revisions";
	public static final String REQUEST_PUT = "put";
	public static final String REQUEST_DELETE = "delete";
	//public static final String REQUEST_UPDATE = "update";
	public static final String REQUEST_AUTHENTICATE = "authenticate";
	public static final String REQUEST_REGISTER = "register";
	public static final String REQUEST_UNREGISTER = "unregister";

	private static Gson gson = initializeGson();

	private static Gson initializeGson() {

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(GetRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(GetResponse.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(GetIndicesRequest.class, new TypeAdapterProtobuf());
    gsonBuilder.registerTypeAdapter(GetIndicesResponse.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(GetRevisionsRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(GetRevisionsResponse.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(PutRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(DeleteRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(RegisterRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(UnregisterRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(AuthenticateRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(ByteString.class, new TypeAdapterByteString());
		gsonBuilder.setPrettyPrinting();
		return gsonBuilder.create();
	}

	@SuppressWarnings("serial")
	public static class JsonConversionException extends Exception {
		JsonConversionException(String msg) {
			super(msg);
		}
	}

	public static String toJson(GeneratedMessage src){
	  return gson.toJson(src);
	}

  private static <T extends GeneratedMessage> T fromJson(String json, Class<T> clz)
      throws JsonConversionException {
    try {
      return gson.fromJson(json, clz);
    } catch (JsonSyntaxException jse) {
      throw new JsonConversionException("Invalid JSON syntax");
    } catch (JsonParseException jse) {
      throw new JsonConversionException("Unable to parse JSON fields into correct message format");
    }
  }

	public static GetRequest getRequestAsProtobuf(String serverName, SchnorrSign signer, byte[] index, byte[] revision) throws NigoriCryptographyException {

    if (revision != null) {
      return GetRequest.newBuilder()
          .setAuth(authenticateRequestAsProtobuf(serverName, signer, toBytes(REQUEST_GET), index, revision))
          .setKey(ByteString.copyFrom(index))
          .setRevision(ByteString.copyFrom(revision)).build();
    } else {
      return GetRequest.newBuilder()
          .setAuth(authenticateRequestAsProtobuf(serverName, signer, toBytes(REQUEST_GET), index))
          .setKey(ByteString.copyFrom(index)).build();
    }

	}

	public static String getRequestAsJson(String serverName, SchnorrSign signer, byte[] index, byte[] revision) throws NigoriCryptographyException {
		return gson.toJson(getRequestAsProtobuf(serverName, signer, index, revision));
	}

	public static GetRequest getRequestFromJson(String json)  throws JsonConversionException {
	  return fromJson(json, GetRequest.class);
	}


	public static GetResponse getResponseAsProtobuf(Collection<RevValue> revisions) {
	  // TODO(drt24) add index
	  List<RevisionValue> protoRevisions = new ArrayList<RevisionValue>(revisions.size());
	  for (RevValue rv : revisions){
	    protoRevisions.add(RevisionValue.newBuilder().setRevision(ByteString.copyFrom(rv.getRevision().getBytes())).setValue(ByteString.copyFrom(rv.getValue())).build());
	  }
	  GetResponse resp = GetResponse.newBuilder()
	      .addAllRevisions(protoRevisions)
	      .build();

		return resp;
	}

	public static String getResponseAsJson(Collection<RevValue> revisions) {
		return gson.toJson(getResponseAsProtobuf(revisions));
	}

	public static GetResponse getResponseFromJson(String json) throws JsonConversionException {
	  return fromJson(json, GetResponse.class);
	}

	public static GetIndicesRequest getIndicesRequestAsProtobuf(String serverName, SchnorrSign signer) throws NigoriCryptographyException {
    return GetIndicesRequest.newBuilder().setAuth(authenticateRequestAsProtobuf(serverName, signer,toBytes(REQUEST_GET_INDICES))).build();
  }

  public static String getIndicesRequestAsJson(String serverName, SchnorrSign signer) throws NigoriCryptographyException {
    return gson.toJson(getIndicesRequestAsProtobuf(serverName, signer));
  }

  public static GetIndicesRequest getIndicesRequestFromJson(String json) throws JsonConversionException {
    return fromJson(json, GetIndicesRequest.class);
  }

  public static GetIndicesResponse getIndicesResponseAsProtobuf(Collection<byte[]> value) {
    List<ByteString> values = new ArrayList<ByteString>(value.size());
    for (byte[] valueA : value){
      values.add(ByteString.copyFrom(valueA));
    }
    return GetIndicesResponse.newBuilder().addAllIndices(values).build();
  }

  public static String getIndicesResponseAsJson(Collection<byte[]> value) {
    return gson.toJson(getIndicesResponseAsProtobuf(value));
  }

  public static GetIndicesResponse getIndicesResponseFromJson(String json) throws JsonConversionException {
    return fromJson(json, GetIndicesResponse.class);
  }

  public static GetRevisionsRequest getRevisionsRequestAsProtobuf(String serverName, SchnorrSign signer, byte[] index) throws NigoriCryptographyException {
    return GetRevisionsRequest.newBuilder()
        .setAuth(authenticateRequestAsProtobuf(serverName, signer,toBytes(REQUEST_GET_REVISIONS),index))
        .setKey(ByteString.copyFrom(index)).build();
  }

  public static String getRevisionsRequestAsJson(String serverName, SchnorrSign signer, byte[] encIndex) throws NigoriCryptographyException {
    return gson.toJson(getRevisionsRequestAsProtobuf(serverName, signer, encIndex));
  }

  public static GetRevisionsRequest getRevisionsRequestFromJson(String json) throws JsonConversionException{
    return fromJson(json, GetRevisionsRequest.class);
  }

  public static GetRevisionsResponse getRevisionsResponseAsProtobuf(Collection<byte[]> value) {
    List<ByteString> values = new ArrayList<ByteString>(value.size());
    for (byte[] valueA : value){
      values.add(ByteString.copyFrom(valueA));
    }
    return GetRevisionsResponse.newBuilder().addAllRevisions(values).build();
  }

  public static String getRevisionsResponseAsJson(Collection<byte[]> value){
    return gson.toJson(getRevisionsResponseAsProtobuf(value));
  }

  public static GetRevisionsResponse getRevisionsResponseFromJson(String json)
      throws JsonConversionException {
    return fromJson(json, GetRevisionsResponse.class);
  }

  public static PutRequest putRequestAsProtobuf(String serverName, SchnorrSign signer, byte[] index, byte[] revision, byte[] value) throws NigoriCryptographyException {

	  PutRequest.Builder reqBuilder = PutRequest.newBuilder()
	      .setAuth(authenticateRequestAsProtobuf(serverName, signer,toBytes(REQUEST_PUT),index,revision,value))
	      .setKey(ByteString.copyFrom(index))
	      .setRevision(ByteString.copyFrom(revision))
	      .setValue(ByteString.copyFrom(value));

		PutRequest req = reqBuilder.build();

		return req;
	}

	public static String putRequestAsJson(String serverName, SchnorrSign signer, byte[] index, byte[] revision, byte[] value) throws	NigoriCryptographyException {
		return gson.toJson(putRequestAsProtobuf(serverName, signer, index, revision, value));
	}

	public static PutRequest putRequestFromJson(String json) throws JsonConversionException {
	  return fromJson(json, PutRequest.class);
	}

	public static DeleteRequest deleteRequestAsProtobuf(String serverName, SchnorrSign signer, byte[] index) throws NigoriCryptographyException{
	  DeleteRequest.Builder delBuilder = DeleteRequest.newBuilder()
	      .setAuth(authenticateRequestAsProtobuf(serverName, signer,toBytes(REQUEST_DELETE),index))
	      .setKey(ByteString.copyFrom(index));

	  DeleteRequest del = delBuilder.build();

	  return del;
	}

	public static String deleteRequestAsJson(String serverName, SchnorrSign signer, byte[] index) throws NigoriCryptographyException {
    return gson.toJson(deleteRequestAsProtobuf(serverName, signer,index));
  }

	public static DeleteRequest deleteRequestFromJson(String json) throws JsonConversionException {
	  return fromJson(json, DeleteRequest.class);
  }

  public static AuthenticateRequest authenticateRequestAsProtobuf(String serverName, SchnorrSign signer)
      throws NigoriCryptographyException {
    return authenticateRequestAsProtobuf(serverName, signer, toBytes(REQUEST_AUTHENTICATE));
  }

  protected static AuthenticateRequest authenticateRequestAsProtobuf(String serverName, SchnorrSign signer,
      byte[]... payload) throws NigoriCryptographyException {

	  try {
	    Nonce nonce = new Nonce();
	    SchnorrSignature signedNonce = signer.sign(Util.joinBytes(MessageLibrary.toBytes(serverName),nonce.nt(),nonce.nr(),Util.joinBytes(payload)));

	    AuthenticateRequest req = AuthenticateRequest.newBuilder()
	        .setPublicKey(ByteString.copyFrom(signer.getPublicKey()))
	        .setSchnorrE(ByteString.copyFrom(signedNonce.getE()))
	        .setSchnorrS(ByteString.copyFrom(signedNonce.getS()))
	        .setNonce(ByteString.copyFrom(nonce.toToken()))
	        .setServerName(serverName)
	        .build();

	    return req;
	  } catch (NoSuchAlgorithmException e) {
	    throw new NigoriCryptographyException("Platform does have required crypto support:" + e.getMessage());
	  }
	}

	public static String authenticateRequestAsJson(String serverName, SchnorrSign signer) throws NigoriCryptographyException {
		return gson.toJson(authenticateRequestAsProtobuf(serverName, signer));
	}

	public static AuthenticateRequest authenticateRequestFromJson(String json) throws
	JsonConversionException {
	  return fromJson(json, AuthenticateRequest.class);
	}

	public static RegisterRequest registerRequestAsProtobuf( SchnorrSign signer, byte[] token ) {

		RegisterRequest req = RegisterRequest.newBuilder()
		    .setPublicKey(ByteString.copyFrom(signer.getPublicKey()))
		    .setToken(ByteString.copyFrom(token))
		    .build();

		return req;
	}

	public static String registerRequestAsJson(SchnorrSign signer, byte[] token) {
		return gson.toJson(registerRequestAsProtobuf(signer, token));
	}

	public static RegisterRequest registerRequestFromJson(String json) throws JsonConversionException {
	  return fromJson(json, RegisterRequest.class);
  }

  public static UnregisterRequest unregisterRequestAsProtobuf(String serverName, SchnorrSign signer) throws NigoriCryptographyException {

    UnregisterRequest req = UnregisterRequest.newBuilder()
        .setAuth(authenticateRequestAsProtobuf(serverName, signer,toBytes(REQUEST_UNREGISTER)))
        .build();

    return req;
  }

	public static String unregisterRequestAsJson(String serverName, SchnorrSign signer) throws NigoriCryptographyException {
    return gson.toJson(unregisterRequestAsProtobuf(serverName, signer));
  }

  public static UnregisterRequest unregisterRequestFromJson(String json)
      throws JsonConversionException {
    return fromJson(json, UnregisterRequest.class);
  }
}
