package com.sparta.whereismyparcel.inventory.domain.exception;

public class ProductVariantNotFoundException extends RuntimeException {
  public ProductVariantNotFoundException(String message) {
    super(message);
  }
}
