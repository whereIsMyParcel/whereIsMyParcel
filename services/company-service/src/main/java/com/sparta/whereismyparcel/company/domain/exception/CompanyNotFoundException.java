package com.sparta.whereismyparcel.company.domain.exception;

public class CompanyNotFoundException extends RuntimeException {
  public CompanyNotFoundException(String message) {
    super(message);
  }
}
