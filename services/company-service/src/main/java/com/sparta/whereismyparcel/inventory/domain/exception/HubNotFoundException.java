package com.sparta.whereismyparcel.inventory.domain.exception;

public class HubNotFoundException extends RuntimeException {
  public HubNotFoundException(String message) {
    super(message);
  }
}
