/*
 * Copyright (C) 2011 Daniel R. Thomas (drt24)
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

import java.security.Principal;
import java.util.Date;

/**
 * Interface for objects representing users detailing their public key and registration date.
 * @author drt24
 * 
 */
public interface User extends Principal {

  /**
   * Handle for representing the user, based on their public key - a human supplied identifier
   * should never reach the server unencrypted.
   */
  @Override
  String getName();
  /**
   * 
   * @return the public key for the user
   */
  byte[] getPublicKey();

  /**
   * 
   * @return the date the user registered their public key
   */
  Date getRegistrationDate();

}
