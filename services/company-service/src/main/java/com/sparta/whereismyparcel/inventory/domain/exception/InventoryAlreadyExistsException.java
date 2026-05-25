package com.sparta.whereismyparcel.inventory.domain.exception;

public class InventoryAlreadyExistsException extends RuntimeException {
  public InventoryAlreadyExistsException(String message) {
    super(message);
  }
}
