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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.google.nigori.common.Index;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.NigoriCryptographyException;
import com.google.nigori.common.NigoriMessages.GetIndicesResponse;
import com.google.nigori.common.NigoriMessages.GetResponse;
import com.google.nigori.common.NigoriMessages.GetRevisionsResponse;
import com.google.nigori.common.NigoriMessages.RevisionValue;
import com.google.nigori.common.NigoriProtocol;
import com.google.nigori.common.NotFoundException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;
import com.google.nigori.common.UnauthorisedException;
import com.google.protobuf.ByteString;

/**
 * A client API capable of managing a session with a Nigori Server.
 * 
 * @author Alastair Beresford
 *
 * It is worth looking at {@link java.util.Collection} but we can't implement that or most of the methods in it until we have a "list indexes" method which at least for now we don't intend to do.
 * putAll from {@link java.util.Map} might be worth implementing
 */
public class CryptoNigoriDatastore implements NigoriDatastore {

	private final KeyManager keyManager;

	private final NigoriProtocol protocol;

	public CryptoNigoriDatastore(NigoriProtocol protocol, String username, String password, String serverName) throws UnsupportedEncodingException, NigoriCryptographyException {
	  this.protocol = protocol;
	  this.keyManager = new RealKeyManager(serverName.getBytes(MessageLibrary.CHARSET),
        username.getBytes(MessageLibrary.CHARSET),
        password.getBytes(MessageLibrary.CHARSET));
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
	public CryptoNigoriDatastore(String server, int port, String serverPrefix, String username,
			String password) throws NigoriCryptographyException, UnsupportedEncodingException {
		String servername = server + ":" + port;
		protocol = new JsonHTTPProtocol(server,port,serverPrefix);
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
	public CryptoNigoriDatastore(String server, int port, String serverPrefix) throws 
	NigoriCryptographyException, UnsupportedEncodingException {
		String servername = server + ":" + port;
		protocol = new JsonHTTPProtocol(server,port,serverPrefix);

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
  public boolean authenticate() throws IOException, NigoriCryptographyException {
    return protocol.authenticate(MessageLibrary.authenticateRequestAsProtobuf(keyManager.signer()));
  }

	@Override
	public boolean register() throws IOException, NigoriCryptographyException {
	  byte[] token = {};
	  return protocol.register(MessageLibrary.registerRequestAsProtobuf(keyManager.signer(), token));
	}

	@Override
  public boolean unregister() throws IOException, NigoriCryptographyException, UnauthorisedException {
	  return protocol.unregister(MessageLibrary.unregisterRequestAsProtobuf(keyManager.signer()));
  }

	@Override
	public List<RevValue> get(Index index) throws IOException,	NigoriCryptographyException, UnauthorisedException {
		return get(null, index, null);
	}

	/**
   * @param index
   * @param revision
   * @return
   * @throws NigoriCryptographyException 
   * @throws IOException 
	 * @throws UnauthorisedException 
   */
	@Override
  public byte[] getRevision(Index index, Revision revision) throws IOException, NigoriCryptographyException, UnauthorisedException {
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
	private List<RevValue> get(byte[] encKey, Index index, Revision revision) throws IOException, NigoriCryptographyException, UnauthorisedException {

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

    try {
      GetResponse getResponse =
          protocol.get(MessageLibrary.getRequestAsProtobuf(keyManager.signer(), encIndex, encRevision));
      if (getResponse == null){
        return null;
      }
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
    } catch (NotFoundException e) {
      return null;
    }
	}

  @Override
  public List<Index> getIndices() throws NigoriCryptographyException, IOException, UnauthorisedException {
  
    try {
      GetIndicesResponse getResponse =
          protocol.getIndices(MessageLibrary.getIndicesRequestAsProtobuf(keyManager.signer()));
      if (getResponse == null){
        return null;
      }
      List<ByteString> indices = getResponse.getIndicesList();
      List<Index> answer = new ArrayList<Index>(indices.size());
      for (ByteString index : indices) {
        answer.add(new Index(keyManager.decrypt(index.toByteArray())));
      }
      return answer;
    } catch (NotFoundException e) {
      return null;
    }
  }

  @Override
  public List<Revision> getRevisions(Index index) throws NigoriCryptographyException,
      UnsupportedEncodingException, IOException, UnauthorisedException {
    byte[] encIndex = keyManager.encryptDeterministically(index.getBytes());

    try {
      GetRevisionsResponse getResponse =
          protocol.getRevisions(MessageLibrary.getRevisionsRequestAsProtobuf(keyManager.signer(), encIndex));
      if (getResponse == null){
        return null;
      }
      List<ByteString> revisions = getResponse.getRevisionsList();
      List<Revision> answer = new ArrayList<Revision>(revisions.size());
      for (ByteString revision : revisions) {
        answer.add(new Revision(keyManager.decrypt(revision.toByteArray())));
      }
      return answer;

    } catch (NotFoundException e) {
      return null;
    }
  }

	@Override
  public boolean put(Index index, Revision revision, byte[] value)  throws IOException, NigoriCryptographyException, UnauthorisedException {
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
   * @throws UnauthorisedException 
   */
  private boolean put(byte[] encKey, Index index, Revision revision, byte[] value) throws IOException, 
  NigoriCryptographyException, UnauthorisedException {
  
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
  	return protocol.put(MessageLibrary.putRequestAsProtobuf(keyManager.signer(), encIndex, encRevision, encValue));
  }

  @Override
  public boolean delete(Index index, byte[] token) throws UnsupportedEncodingException, NigoriCryptographyException, IOException, UnauthorisedException {
    return delete(null, index, token);
  }

	private boolean delete(byte[] encKey, Index index, byte[] token) throws NigoriCryptographyException, UnsupportedEncodingException, IOException, UnauthorisedException {
	  byte[] encIndex;
	  if (encKey == null) {
	    encIndex = keyManager.encryptDeterministically(index.getBytes());
	  } else {
	    encIndex = keyManager.encryptDeterministically(encKey, index.getBytes());
	  }
	  try{
	    return protocol.delete(MessageLibrary.deleteRequestAsProtobuf(keyManager.signer(), encIndex));
	  } catch (NotFoundException e) {
      return false;
    }
	}

}