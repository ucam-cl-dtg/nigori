package com.google.nigori.server;


public class UserNotFoundException extends Exception {

  public UserNotFoundException(Exception e) {
    super(e);
  }

  public UserNotFoundException() {
    super();
  }

  private static final long serialVersionUID = 2L;

}
