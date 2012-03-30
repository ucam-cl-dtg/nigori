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
package com.google.nigori.server.appengine;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author drt24
 * 
 */
public class CleanNoncesServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) {
    String isCron = req.getHeader("X-AppEngine-Cron");
    if (isCron != null && isCron.contains("true")) {
      (new AppEngineDatabase()).clearOldNonces();
      try {
        resp.getOutputStream().write("Cleared nonces".getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      try {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
