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
package com.google.nigori.client;

import java.io.IOException;
import java.util.List;

import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.RevValue;

/**
 * Create two threads which communicate by placing encrypted data in the Nigori datastore.
 * 
 * @author Alastair Beresford
 *
 */
public class TwoUserDemo {

  protected static final int PORT = 8888;
  protected static final String HOST = "localhost";
  private static final int ITERATIONS = 60;
  private static final int DELAY = 100;
	private static class SeparateUserAccessesSharedStore extends Thread {
		
		private String username;
		private String password;
		private Index sharedIndex;
		
		SeparateUserAccessesSharedStore(String username, String password, Index sharedIndex) {

			this.username = username;
			this.password = password;
			this.sharedIndex =sharedIndex;
		}

		public void run() {
			byte count = 0;
			try {
				NigoriDatastore sharedStore = new HTTPNigoriDatastore(HOST, PORT, "nigori", username,
						password);
				for(int i = 0; i < ITERATIONS; ++i) {
					sharedStore.put(sharedIndex, new byte[]{}, new byte[]{count++});
					sleep(DELAY * 2);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			} catch (NigoriCryptographyException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) throws NigoriCryptographyException, IOException {

		//This auto-generates a unique username and password used in the store.
		//The username and password is the data which should be shared between two devices.
		final HTTPNigoriDatastore sharedStore = new HTTPNigoriDatastore(HOST, PORT, "nigori");
		final String username = sharedStore.getUsername();
		final String password = sharedStore.getPassword();
		System.out.println("Shared store: Username='" + username + "' Password='" + password + "'");

		if(!sharedStore.register()) {
			System.out.println("Failed to register shared store");
			return;
		}		

		//This is the index in the store which is used to hold the shared data
		final Index sharedIndex = new Index(new byte[]{1});

		SeparateUserAccessesSharedStore secondUser = new SeparateUserAccessesSharedStore(username, 
				password, sharedIndex);		
		secondUser.start();
		
		//First user, possibly on a different device
		for(int i = 0; i < ITERATIONS; ++i) {
		  List<RevValue> results = sharedStore.get(sharedIndex);
      if (results == null || results.isEmpty()) {
        System.out.println("No valid data held");
      } else {
        for (RevValue rv : results) {
          byte[] result = rv.getValue();
          if (result == null) {
            System.out.println("No valid data held for " + rv.getRevision());
          } else {
            System.out.println("Count has the value " + result[0] + " for revision "
                + new String(rv.getRevision(), MessageLibrary.CHARSET));
          }
        }
      }
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    try {
      secondUser.join();
    } catch (InterruptedException e) {
    }
    // Clean up
    sharedStore.delete(sharedIndex, new byte[]{});
    sharedStore.unregister();

  }
}
