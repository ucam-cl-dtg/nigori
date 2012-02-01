/*
 * Copyright (C) 2011 Daniel R. Thomas (drt24)
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.google.nigori.common.Nonce;
import com.google.nigori.common.RevValue;

/**
 * TODO(drt24) plumb this in, test it and comment it
 * @author drt24
 *
 */
public class SQLDatabase implements Database {

  private Connection con;
  private Logger log = Logger.getLogger("SQLDatabase");

  public SQLDatabase() throws ClassNotFoundException, SQLException {
    Class.forName("org.postgresql.Driver");// loads the driver needed
    con = DriverManager.getConnection("jdbc:postgresql:nigori", "nigori", "");
    // makes the connection
    // TODO(drt24) read information for this from config
  }

  @Override
  public void finalize() throws SQLException {
    con.close();
  }

  @Override
  public UserFactory getUserFactory() {
    return JUser.Factory.getInstance();
  }

  @Override
  public boolean addUser(byte[] publicKey) {
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement("INSERT INTO stores (pk) VALUES (?)");
      statement.setBytes(1, publicKey);
      statement.executeUpdate();

      return true;
    } catch (SQLException e) {
      log.warning(e.getMessage());
      return false;
    } finally {
      if (statement != null)
        try {
          statement.close();
        } catch (SQLException e) {
          log.fine(e.getMessage());
        }
    }
  }

  @Override
  public boolean haveUser(byte[] existingUser) {
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement("SELECT sid FROM stores WHERE pk = ?");
      statement.setBytes(1, existingUser);

      ResultSet results = statement.executeQuery();
      return results.first();
    } catch (SQLException e) {
      log.warning(e.getMessage());
      return false;
    } finally {
      if (statement != null)
        try {
          statement.close();
        } catch (SQLException e) {
          log.fine(e.getMessage());
        }
    }
  }

  @Override
  public boolean deleteUser(User existingUser) {
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement("DELETE FROM stores WHERE pk = ?");
      statement.setBytes(1, existingUser.getPublicKey());
      statement.executeUpdate();

      ResultSet results = statement.getResultSet();
      return results.first();
    } catch (SQLException e) {
      log.warning(e.getMessage());
      return false;
    } finally {
      if (statement != null)
        try {
          statement.close();
        } catch (SQLException e) {
          log.fine(e.getMessage());
        }
    }
  }

  @Override
  public boolean checkAndAddNonce(Nonce nonce, byte[] publicKey) {
    PreparedStatement queryStatement = null, insertStatement = null;
    try {//TODO
      queryStatement = con.prepareStatement("SELECT sid FROM stores WHERE pk = ?");
      int sid;
      try {
        queryStatement.setBytes(1, publicKey);
        ResultSet set = queryStatement.executeQuery();
        if (!set.first()) {
          return false;
        }
        sid = set.getInt("sid");
      } finally {
        queryStatement.close();
      }
      insertStatement = con.prepareStatement("INSERT INTO nonces (sid, nonce, use) VALUES (?,?,?)");
      try {
        insertStatement.setInt(1, sid);
        insertStatement.setBytes(2, nonce.toToken());
        insertStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
        return insertStatement.executeUpdate() > 0;
      } finally {
        insertStatement.close();
      }
    } catch (SQLException e) {
      log.warning(e.getMessage());
      return false;
    }
  }

  @Override
  public User getUser(byte[] publicKey) throws UserNotFoundException {
    try {
      PreparedStatement queryStatement = con.prepareStatement("SELECT pk, reg FROM stores WHERE pk = ?");
      try {
        queryStatement.setBytes(1, publicKey);
        ResultSet set = queryStatement.executeQuery();
        if (!set.first()) {
          throw new UserNotFoundException();
        }
        byte[] pk = set.getBytes("pk");
        Timestamp reg = set.getTimestamp("reg");
        return new JUser(pk, new Date(reg.getTime()));
      } finally {
        queryStatement.close();
      }
    } catch (SQLException e) {
      log.severe(e.toString());
      throw new UserNotFoundException(e);
    }
  }

  @Override
  public Collection<RevValue> getRecord(User user, byte[] key) throws IOException {
    try {
      PreparedStatement queryStatement = con.prepareStatement("SELECT rev, val FROM rev_values, lookups, stores WHERE rev_values.lid = lookups.lid AND lookups.lookup = ? AND lookups.sid = stores.sid AND stores.pk =?");
      try {
        queryStatement.setBytes(1, key);
        queryStatement.setBytes(2, user.getPublicKey());
        ResultSet set = queryStatement.executeQuery();

        List<RevValue> revValues = new ArrayList<RevValue>();
        while (set.next()){
          byte[] rev = set.getBytes("rev");
          byte[] value = set.getBytes("val");
          revValues.add(new RevValue(rev,value));
        }
        return revValues;
      } finally {
        queryStatement.close();
      }

    } catch (SQLException e){
      throw new IOException(e);
    }
  }

  @Override
  public Collection<byte[]> getRevisions(User user, byte[] key) throws IOException {
    try {
      PreparedStatement queryStatement = con.prepareStatement("SELECT rev FROM revisions, lookups, stores WHERE revisions.lid = lookups.lid AND lookups.lookup = ? AND lookups.sid = stores.sid AND stores.pk = ?");
      try {
        queryStatement.setBytes(1, key);
        queryStatement.setBytes(2, user.getPublicKey());
        ResultSet set = queryStatement.executeQuery();

        List<byte[]> revValues = new ArrayList<byte[]>();
        while (set.next()){
          byte[] rev = set.getBytes("rev");
          revValues.add(rev);
        }
        return revValues;
      } finally {
        queryStatement.close();
      }

    } catch (SQLException e){
      throw new IOException(e);
    }
  }

  @Override
  public boolean putRecord(User user, byte[] key, byte[] revision, byte[] data) {
    try {// TODO(drt24) this needs to use transactions.
      PreparedStatement getLid = con.prepareStatement("SELECT lid FROM lookups, stores WHERE lookups.sid = stores.sid AND lookups.lookup = ? AND stores.pk = ?");
      int lid;
      try {
        getLid.setBytes(1, key);
        getLid.setBytes(2, user.getPublicKey());
        ResultSet set = getLid.executeQuery();
        
        if (!set.first()){// if there is no lid for this lookup then store the lookup and get the lid
          set.close();
          PreparedStatement createLookup = con.prepareStatement("INSERT INTO lookups (sid,lookup) SELECT sid, ? FROM stores WHERE stores.pk = ? RETURNING lid");
          try {
            createLookup.setBytes(1, key);
            createLookup.setBytes(2,  user.getPublicKey());
            createLookup.execute();
            ResultSet lookupSet = createLookup.getResultSet();
            if (!lookupSet.first()) {
              log.severe("Insertion of lookup did not succeed");
              return false;
            }
            lid = lookupSet.getInt("lid");
          } finally {
            createLookup.close();
          }
          

        } else {
          lid = set.getInt("lid");
        }
      } finally {
        getLid.close();
      }

      // TODO(drt24) only need to do this if the revision does not exist
      PreparedStatement insertRevision = con.prepareStatement("INSERT INTO revisions (lid, rev) VALUES (?, ?) RETURNING rid");
      int rid;
      try {
        insertRevision.setInt(1, lid);
        insertRevision.setBytes(2, revision);
        insertRevision.execute();
        ResultSet ridSet = insertRevision.getResultSet();
        if (!ridSet.first()) {
          log.severe("Inserting revision failed");
          return false;
        }
        rid = ridSet.getInt("rid");
      } finally {
        insertRevision.close();
      }
      
      PreparedStatement insertValue = con.prepareStatement("INSERT INTO rid_values VALUES (?, ?)");
      insertValue.setInt(1, rid);
      insertValue.setBytes(2, data);
      return insertValue.executeUpdate() == 1;

    } catch (SQLException e){
      log.severe(e.toString());
      return false;
    }
  }

  @Override
  public boolean deleteRecord(User user, byte[] key) {
    try {
      PreparedStatement deleteKey = con.prepareStatement("DELETE FROM lookups USING stores WHERE lookups.sid = stores.sid AND stores.pk = ? AND lookup = ?");
      try {
        deleteKey.setBytes(1, user.getPublicKey());
        deleteKey.setBytes(2, key);
        return deleteKey.executeUpdate() > 0;
      } finally {
        deleteKey.close();
      }
    } catch (SQLException e){
      log.severe(e.toString());
      return false;
    }
  }

  @Override
  public void clearOldNonces() {
    // TODO Auto-generated method stub
    
  }

}
