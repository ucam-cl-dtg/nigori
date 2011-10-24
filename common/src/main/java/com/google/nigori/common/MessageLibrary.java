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

import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.nigori.common.NigoriMessages.AuthenticateRequest;
import com.google.nigori.common.NigoriMessages.DeleteRequest;
import com.google.nigori.common.NigoriMessages.GetRequest;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.PutRequest;
import com.google.nigori.common.NigoriMessages.PutRequest.Builder;
import com.google.nigori.common.NigoriMessages.RegisterRequest;
import com.google.nigori.common.NigoriMessages.UnregisterRequest;
import com.google.protobuf.ByteString;

public class MessageLibrary {

	//The character set used to encode all string-based communications in Nigori.
	public static final String CHARSET = "UTF-8";

	//The mimetypes used for all supported communication formats in Nigori
	public static final String MIMETYPE_JSON = "application/json";
	public static final String MIMETYPE_PROTOBUF = "application/x-google-protobuf";

	public static final String REQUEST_GET = "get";
	public static final String REQUEST_PUT = "put";
	public static final String REQUEST_DELETE = "delete";
	public static final String REQUEST_UPDATE = "update";
	public static final String REQUEST_AUTHENTICATE = "authenticate";
	public static final String REQUEST_REGISTER = "register";
	public static final String REQUEST_UNREGISTER = "unregister";

	private static Gson gson = initializeGson();

	private static Gson initializeGson() {

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(GetRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(GetResponse.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(PutRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(DeleteRequest.class, new TypeAdapterProtobuf());
		gsonBuilder.registerTypeAdapter(RegisterRequest.class, new TypeAdapterProtobuf());
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

	public static GetRequest getRequestAsProtobuf(byte[] username, SchnorrSign signer, byte[] key) throws 
	NoSuchAlgorithmException {

		SchnorrSignature signedNonce = signer.sign(new Nonce().toToken());

		AuthenticateRequest.Builder authBuilder = AuthenticateRequest.newBuilder()
        .setUsername(ByteString.copyFrom(username))
        .setSchnorrE(ByteString.copyFrom(signedNonce.getE()))
        .setSchnorrS(ByteString.copyFrom(signedNonce.getS()))
        .setNonce(ByteString.copyFrom(signedNonce.getMessage()));
		
		GetRequest req = GetRequest.newBuilder()
		.setAuth(authBuilder.build())
		.setKey(ByteString.copyFrom(key))
		.build();

		return req;
	}

	public static String getRequestAsJson(byte[] username, SchnorrSign signer, byte[] key) throws
	NoSuchAlgorithmException {
		return gson.toJson(getRequestAsProtobuf(username, signer, key));
	}

	public static GetRequest getRequestFromJson(String json)  throws JsonConversionException {
		try {
			return gson.fromJson(json, GetRequest.class);
		} catch (JsonSyntaxException jse) {
			throw new JsonConversionException("Invalid JSON syntax");
		} catch (JsonParseException jse) {
			throw new JsonConversionException("Unable to parse JSON fields into correct message format");
		}
	}


	public static GetResponse getResponseAsProtobuf(byte[] value) {

		GetResponse resp = GetResponse.newBuilder()
		.setValue(ByteString.copyFrom(value))
		.build();

		return resp;
	}

	public static String getResponseAsJson(byte[] value) {
		return gson.toJson(getResponseAsProtobuf(value));
	}

	public static GetResponse getResponseFromJson(String json) throws JsonConversionException {
		try {
			return gson.fromJson(json, GetResponse.class);
		} catch (JsonSyntaxException jse) {
			throw new JsonConversionException("Invalid JSON syntax");
		} catch (JsonParseException jse) {
			throw new JsonConversionException("Unable to parse JSON fields into correct message format");
		}
	}

	public static PutRequest putRequestAsProtobuf(byte[] username, SchnorrSign signer, byte[] key, byte[] value) throws NoSuchAlgorithmException {

		SchnorrSignature signedNonce = signer.sign(new Nonce().toToken());

		AuthenticateRequest.Builder authBuilder = AuthenticateRequest.newBuilder()
		    .setUsername(ByteString.copyFrom(username))
		    .setSchnorrE(ByteString.copyFrom(signedNonce.getE()))
		    .setSchnorrS(ByteString.copyFrom(signedNonce.getS()))
		    .setNonce(ByteString.copyFrom(signedNonce.getMessage()));
		Builder reqBuilder = PutRequest.newBuilder()
		.setAuth(authBuilder.build())
		.setKey(ByteString.copyFrom(key))
		.setValue(ByteString.copyFrom(value));
		
		PutRequest req = reqBuilder.build();

		return req;
	}

	public static String putRequestAsJson(byte[] username, SchnorrSign signer, byte[] key, byte[] value) throws	NoSuchAlgorithmException {
		return gson.toJson(putRequestAsProtobuf(username, signer, key, value));
	}

	public static PutRequest putRequestFromJson(String json) throws JsonConversionException {
		try {
			return gson.fromJson(json, PutRequest.class);
		} catch (JsonSyntaxException jse) {
			throw new JsonConversionException("Invalid JSON syntax");
		} catch (JsonParseException jse) {
			throw new JsonConversionException("Unable to parse JSON fields into correct message format");
		}
	}

	public static AuthenticateRequest authenticateRequestAsProtobuf(byte[] username, SchnorrSign signer) throws
	NoSuchAlgorithmException {

		SchnorrSignature signedNonce = signer.sign(new Nonce().toToken());

		AuthenticateRequest req = AuthenticateRequest.newBuilder()
		.setUsername(ByteString.copyFrom(username))
		.setSchnorrE(ByteString.copyFrom(signedNonce.getE()))
		.setSchnorrS(ByteString.copyFrom(signedNonce.getS()))
		.setNonce(ByteString.copyFrom(signedNonce.getMessage()))
		.build();

		return req;
	}

	public static String authenticateRequestAsJson(byte[] username, SchnorrSign signer) throws
	NoSuchAlgorithmException {
		return gson.toJson(authenticateRequestAsProtobuf(username, signer));
	}

	public static AuthenticateRequest authenticateRequestFromJson(String json) throws
	JsonConversionException {
		try {
			return gson.fromJson(json, AuthenticateRequest.class);
		} catch (JsonSyntaxException jse) {
			throw new JsonConversionException("Invalid JSON syntax");
		} catch (JsonParseException jse) {
			throw new JsonConversionException("Unable to parse JSON fields into correct message format");
		}
	}

	public static RegisterRequest registerRequestAsProtobuf(byte[] user, SchnorrSign signer) throws
	NoSuchAlgorithmException {

		RegisterRequest req = RegisterRequest.newBuilder()
		    .setUser(ByteString.copyFrom(user))
		    .setPublicKey(ByteString.copyFrom(signer.getPublicKey()))
		    .build();

		return req;
	}

	public static String registerRequestAsJson(byte[] user, SchnorrSign signer) throws
	NoSuchAlgorithmException {
		return gson.toJson(registerRequestAsProtobuf(user, signer));
	}

	public static UnregisterRequest unregisterRequestAsProtobuf(byte[] user, SchnorrSign signer) throws
  NoSuchAlgorithmException {

    UnregisterRequest req = UnregisterRequest.newBuilder()
        .setUser(ByteString.copyFrom(user))
        .setPublicKey(ByteString.copyFrom(signer.getPublicKey()))
        .build();

    return req;
  }

	public static String unregisterRequestAsJson(byte[] user, SchnorrSign signer) throws
  NoSuchAlgorithmException {
    return gson.toJson(unregisterRequestAsProtobuf(user, signer));
  }

	public static RegisterRequest registerRequestFromJson(String json) throws 
	JsonConversionException {
		try {
			return gson.fromJson(json, RegisterRequest.class);
		} catch (JsonSyntaxException jse) {
			throw new JsonConversionException("Invalid JSON syntax");
		} catch (JsonParseException jse) {
			throw new JsonConversionException("Unable to parse JSON fields into correct message format");
		}
	}
}
