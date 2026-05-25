package com.sparta.whereismyparcel.company.domain.exception;

public class AlreadyRegisterMemberException extends RuntimeException {
  public AlreadyRegisterMemberException(String message) {
    super(message);
  }
}
