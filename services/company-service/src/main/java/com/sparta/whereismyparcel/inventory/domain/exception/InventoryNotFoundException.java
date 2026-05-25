package com.sparta.whereismyparcel.inventory.domain.exception;

public class InventoryNotFoundException extends RuntimeException {
  public InventoryNotFoundException(String message) {
    super(message);
  }
}
