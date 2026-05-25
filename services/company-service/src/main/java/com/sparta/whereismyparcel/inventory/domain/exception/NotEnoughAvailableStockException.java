package com.sparta.whereismyparcel.inventory.domain.exception;

public class NotEnoughAvailableStockException extends RuntimeException {
  public NotEnoughAvailableStockException(String message) {
    super(message);
  }
}
