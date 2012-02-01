/*
 * Copyright (C) 2011 Daniel Thomas (drt24)
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
package com.google.nigori.client.accept;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.google.nigori.client.NigoriCryptographyException;
import com.google.nigori.client.HTTPNigoriDatastore;


@RunWith(Suite.class)
@Suite.SuiteClasses ({
  RegistrationTest.class,
  SetGetDeleteTest.class,
  ConcurrencyTest.class,
  CommandLineExampleTest.class,
  TwoUserDemoTest.class
  })
public class AcceptanceTests {

  protected static final int PORT = 8888;
  protected static final String HOST = "localhost";
  private static long startTime;
  private static final int EXTERNAL_TIMEOUT = 20000;
  public static final int REPEAT = 3;

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
        System.out.println("Running: " + cmd);

        File current = new File("").getAbsoluteFile();
        File parent = current.getParentFile();
        pb.directory(new File(parent, "server"));
        pb.redirectErrorStream(true); // use this to capture messages sent to stderr
        Process shell = pb.start();
        try {
          InputStream shellIn = shell.getInputStream(); // this captures the output from the command
          try {
            int shellExitStatus = shell.waitFor(); // wait for the shell to finish and get the return
            // code

            // at this point you can process the output issued by the command
            // for instance, this reads the output and writes it to System.out:
            System.out.println("Exit status:" + shellExitStatus);
          } catch (InterruptedException e) {
            System.out.println("Did not terminate");
          }
          int c;
          while ((c = shellIn.read()) != -1) {
            System.out.write(c);
          }
          // close the stream
          shellIn.close();
        } finally {
          shell.destroy();
        }
      } catch (IOException ignoreMe) {
        ignoreMe.printStackTrace();
      }
    }
  }

  @BeforeClass
  public static void startDevServer() throws Exception {
    startTime = System.currentTimeMillis();
    Thread starter = new GaeThread("start");
    starter.start();
    starter.join(EXTERNAL_TIMEOUT);
    starter.interrupt();
  }

  @AfterClass
  public static void stopDevServer() throws Exception {
    Thread stopper = new GaeThread("stop");
    stopper.start();
    stopper.join(EXTERNAL_TIMEOUT);
    stopper.interrupt();
    System.out.println("Total time (ms): " + ((System.currentTimeMillis() - startTime)));
  }

  public static HTTPNigoriDatastore getStore() throws UnsupportedEncodingException, NigoriCryptographyException {
    return new HTTPNigoriDatastore(HOST, PORT, "nigori");
  }
}
