package com.sparta.whereismyparcel.inventory.domain.exception;

public class InvalidMinusReservedStockException extends RuntimeException {
  public InvalidMinusReservedStockException(String message) {
    super(message);
  }
}
