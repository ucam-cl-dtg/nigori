/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
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
package com.google.nigori.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author drt24
 * 
 */
public class RegistrationTest {

  public static final int PORT = 8080;
  public static final String HOST = "localhost";
  private static Thread starter;
  private static long startTime;
  private static final int EXTERNAL_TIMEOUT = 10000;

  protected static class GaeThread extends Thread {
    private final String gaeCommand;
    public GaeThread(String command){
      gaeCommand = command;
    }
    public void run() {

      try {
        String cmd = "mvn gae:" + gaeCommand; // this is the command to execute in the
                                                      // Unix shell
        // create a process for the shell
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);

        File current = new File("").getAbsoluteFile();
        File parent = current.getParentFile();
        pb.directory(new File(parent, "server"));
        pb.redirectErrorStream(true); // use this to capture messages sent to stderr
        Process shell = pb.start();
        InputStream shellIn = shell.getInputStream(); // this captures the output from the command
        int shellExitStatus = shell.waitFor(); // wait for the shell to finish and get the return
                                               // code
        // at this point you can process the output issued by the command
        // for instance, this reads the output and writes it to System.out:
        System.out.println(shellExitStatus);
        int c;
        while ((c = shellIn.read()) != -1) {
          System.out.write(c);
        }
        // close the stream
        shellIn.close();
      } catch (IOException ignoreMe) {
        ignoreMe.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  @BeforeClass
  public static void startDevServer() throws Exception {
    startTime = System.currentTimeMillis();
    starter = new GaeThread("start");
    starter.start();
    starter.join(EXTERNAL_TIMEOUT);
  }

  @AfterClass
  public static void stopDevServer() throws Exception {
    starter.join(EXTERNAL_TIMEOUT);
    Thread stopper = new GaeThread("stop");
    stopper.start();
    stopper.join(EXTERNAL_TIMEOUT);
    System.out.println("Total time (ms): " + ((System.currentTimeMillis() - startTime)));
  }

  @Test
  public void test() throws NigoriCryptographyException, IOException {
    NigoriDatastore nigori = new NigoriDatastore(HOST, PORT, "nigori");
    for (int i = 0; i < 3; ++i) {// check we can do this more than once
      assertTrue("Not registered", nigori.register());
      assertTrue("Can't authenticate",nigori.authenticate());
      assertTrue("Can't unregister", nigori.unregister());
      assertFalse("Authenticated after unregistration", nigori.authenticate());
      assertFalse("Could re-unregister", nigori.unregister());
    }
  }

}
