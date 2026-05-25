package com.sparta.whereismyparcel.company.domain.exception;

public class UserSyncFailedException extends RuntimeException {
  public UserSyncFailedException(String message) {
    super(message);
  }
}
