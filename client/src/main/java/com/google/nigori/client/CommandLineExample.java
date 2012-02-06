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
import java.io.PrintStream;
import java.util.List;

import com.google.nigori.common.Index;
import com.google.nigori.common.MessageLibrary;
import com.google.nigori.common.RevValue;
import com.google.nigori.common.Revision;

/**
 * 
 * @author Alastair Beresford
 *
 */
public class CommandLineExample {

  private static void usage() {
    PrintStream out = System.err;
    out.println("Usage: java nigori.jar server port <action and args>");
    out.println(" Where <action and args> is one of the following:");
    out.println("  register username password");
    out.println("  unregister username password");
    out.println("  authenticate username password");
    out.println("  put username password index revision value");
    out.println("  get username password index");
    out.println("  delete username password key");
  }	
	
	public static void main(String[] args) throws Exception {

		if (args.length < 5 || args.length > 8) {
			usage();
			return;
		}

		final String server = args[0];
		final int port = Integer.parseInt(args[1]);
		final String action = args[2];
		final String username = args[3];
		final String password = args[4];

		NigoriDatastore nigori = new HTTPNigoriDatastore(server, port, "nigori", username, password);
		
		if (action.equals("register")) {
			boolean success = nigori.register();
			System.out.println("Success: " + success);
    } else if (action.equals("unregister")) {
      boolean success = nigori.unregister();
      System.out.println("Success: " + success);
    }
		else if (action.equals("authenticate")) {
			boolean success = nigori.authenticate();
			System.out.println("Success: " + success);
		}
		else if (action.equals("put")) {
			if (args.length != 8) {
				System.err.println("*** Error: exactly seven elements needed for a put action");
				usage();
				return;
			}
			boolean success = nigori.put(new Index(args[5]), new Revision(args[6]),
					args[7].getBytes(MessageLibrary.CHARSET));
			System.out.println("Success: " + success);
		}
		else if (action.equals("get")) {
			if (args.length != 6) {
				System.err.println("*** Error: exactly six elements needed for a get action");
				usage();
				return;
			}
			try {
				List<RevValue> data = nigori.get(new Index(args[5]));
        for (RevValue datum : data) {
          System.out.println(new String(datum.getRevision().getBytes()) + new String(datum.getValue()));
        }
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
			}
		}
		else if (action.equals("delete")) {
			if (args.length != 6) {
				System.err.println("*** Error: exactly six elements needed for a delete action");
				usage();
				return;
			}
			boolean success = nigori.delete(new Index(args[5]),new byte[]{});
			System.out.println("Success: " + success);
		}
		else {
			System.err.println("*** Error: Unknown action "+action);
			usage();
		}
	}
}
