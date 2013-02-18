/*
 * Copyright (C) 2012 Daniel R. Thomas (drt24)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.nigori.server;

import static com.google.nigori.common.MessageLibrary.toBytes;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import com.google.nigori.common.DSASignature;
import com.google.nigori.common.DSAVerify;
import com.google.nigori.common.MessageLibrary;
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
import com.google.nigori.common.NigoriMessages.UnregisterRequest;
import com.google.nigori.common.NigoriProtocol;
import com.google.nigori.common.Nonce;
import com.google.nigori.common.NotFoundException;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.UnauthorisedException;
import com.google.nigori.common.Util;

/**
 * Take messages from the {@link NigoriProtocol} and translate them to the {@link Database} if they
 * are valid
 * 
 * @author drt24
 * 
 */
public class DatabaseNigoriProtocol implements NigoriProtocol {

  private static final Logger log = Logger.getLogger(DatabaseNigoriProtocol.class.getSimpleName());
  private static void warning(String message, Exception exception){
    log.log(Level.WARNING, message, exception);
  }
  private static void severe(String message, Exception exception){
    log.log(Level.SEVERE, message, exception);
  }
  private final Database database;

  public DatabaseNigoriProtocol(Database database) {
    this.database = database;
  }

  /**
   * Check {@code nonce} is valid. If so, this method returns; else throws a ServletException
   * 
   * @param schnorrS
   * @param schnorrE
   * @param message
   * @throws ServletException
   */
  private User authenticateUser(AuthenticateRequest auth, byte[]... payload)
      throws UnauthorisedException, CryptoException {

    byte[] publicHash = auth.getPublicKey().toByteArray();
    List<byte[]> byteSig = Util.splitBytes(auth.getSig().toByteArray());
    byte[] dsaR = byteSig.get(0);
    byte[] dsaS = byteSig.get(1);
    Nonce nonce = new Nonce(auth.getNonce().toByteArray());
    String serverName = auth.getServerName();
    try {
      byte[] publicKey = database.getPublicKey(publicHash);

      DSASignature sig =
          new DSASignature(dsaR, dsaS, Util.joinBytes(toBytes(serverName), nonce.nt(), nonce.nr(), Util.joinBytes(payload)));
      try {
        DSAVerify v = new DSAVerify(publicKey);

        if (v.verify(sig)) {
          boolean validNonce = database.checkAndAddNonce(nonce, publicHash);

          boolean userExists = database.haveUser(publicHash);
          if (validNonce && userExists) {
            try {
              return database.getUser(publicHash);
            } catch (UserNotFoundException e) {
              // TODO(drt24): potential security vulnerability - user existence oracle.
              // Should not happen often - is only possible due to concurrency
              throw new UnauthorisedException("No such user");
            }
          } else {
            throw new UnauthorisedException("Invalid nonce or no such user");
          }
        } else {
          throw new UnauthorisedException("The signature is invalid");
        }
      } catch (NoSuchAlgorithmException nsae) {
        severe("authenticateUser",nsae);
        throw new CryptoException("Internal error attempting to verify signature");
      }
    } catch (UserNotFoundException e1) {
      warning("authenticateUser",e1);
      throw new UnauthorisedException("No such user");
    }
  }

  @Override
  public boolean authenticate(AuthenticateRequest request) throws IOException {
    try {
      authenticateUser(request,toBytes(MessageLibrary.REQUEST_AUTHENTICATE));
    } catch (UnauthorisedException e) {
      warning("unauthorized authenticate",e);
      return false;
    }
    return true;
  }

  @Override
  public boolean register(RegisterRequest request) throws IOException {
    // TODO(drt24): validate request.getToken()
    byte[] publicKey = request.getPublicKey().toByteArray();
    try {
      return database.addUser(publicKey, Util.hashKey(publicKey));
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean unregister(UnregisterRequest request) throws IOException, UnauthorisedException {
    AuthenticateRequest auth = request.getAuth();
    User user = authenticateUser(auth,toBytes(MessageLibrary.REQUEST_UNREGISTER));

    return database.deleteUser(user);
  }

  @Override
  public GetResponse get(GetRequest request) throws IOException, NotFoundException,
      UnauthorisedException {
    byte[] index = request.getKey().toByteArray();
    AuthenticateRequest auth = request.getAuth();
    User user;
    byte[] revision = null;
    if (request.hasRevision()) {
      revision = request.getRevision().toByteArray();
      user = authenticateUser(auth, toBytes(MessageLibrary.REQUEST_GET), index, revision);
    } else {
      user = authenticateUser(auth, toBytes(MessageLibrary.REQUEST_GET), index);
    }

    Collection<RevValue> value;

    if (request.hasRevision()) {
      
      value = new ArrayList<RevValue>(1);
      RevValue revVal = database.getRevision(user, index, revision);
      if (revVal == null) {
        throw new NotFoundException("Cannot find requested index with revision");
      }
      value.add(revVal);
    } else {
      value = database.getRecord(user, index);
    }
    if (value == null) {
      throw new NotFoundException("No value for that index");
    }
    return MessageLibrary.getResponseAsProtobuf(value);
  }

  @Override
  public GetIndicesResponse getIndices(GetIndicesRequest request) throws IOException,
      NotFoundException, UnauthorisedException {
    AuthenticateRequest auth = request.getAuth();
    User user = authenticateUser(auth,toBytes(MessageLibrary.REQUEST_GET_INDICES));

    Collection<byte[]> value = database.getIndices(user);

    if (value == null) {
      throw new NotFoundException("Cannot find indices");
    }
    return MessageLibrary.getIndicesResponseAsProtobuf(value);
  }

  @Override
  public GetRevisionsResponse getRevisions(GetRevisionsRequest request) throws IOException,
      NotFoundException, UnauthorisedException {
    byte[] index = request.getKey().toByteArray();
    AuthenticateRequest auth = request.getAuth();
    User user = authenticateUser(auth,toBytes(MessageLibrary.REQUEST_GET_REVISIONS),index);

    Collection<byte[]> value = database.getRevisions(user, index);

    if (value == null) {
      throw new NotFoundException("Cannot find requested key");
    }
    return MessageLibrary.getRevisionsResponseAsProtobuf(value);
  }

  @Override
  public boolean put(PutRequest request) throws IOException, UnauthorisedException {
    AuthenticateRequest auth = request.getAuth();

    byte[] index = request.getKey().toByteArray();
    byte[] revision = request.getRevision().toByteArray();
    byte[] value = request.getValue().toByteArray();
    User user = authenticateUser(auth, toBytes(MessageLibrary.REQUEST_PUT), index, revision, value);

    return database.putRecord(user, index, revision, value);
  }

  @Override
  public boolean delete(DeleteRequest request) throws IOException, NotFoundException,
      UnauthorisedException {
    AuthenticateRequest auth = request.getAuth();

    byte[] index = request.getKey().toByteArray();
    User user = authenticateUser(auth,toBytes(MessageLibrary.REQUEST_DELETE),index);

    boolean exists = database.getRecord(user, index) != null;
    if (!exists) {
      throw new NotFoundException("No such index: "
          + Base64.encodeBase64(request.getKey().toByteArray()));
    }

    return database.deleteRecord(user, index);
  }

  public static class CryptoException extends IOException {
    private static final long serialVersionUID = 1L;

    public CryptoException(String message) {
      super(message);
    }
  }
}
