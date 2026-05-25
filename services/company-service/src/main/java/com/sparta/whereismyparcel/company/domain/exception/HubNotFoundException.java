package com.sparta.whereismyparcel.company.domain.exception;

public class HubNotFoundException extends RuntimeException {
  public HubNotFoundException(String message) {
    super(message);
  }
}
