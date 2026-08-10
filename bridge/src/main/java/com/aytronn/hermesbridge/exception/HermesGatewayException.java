package com.aytronn.hermesbridge.exception;

public class HermesGatewayException extends RuntimeException {

  public HermesGatewayException(String message) {
    super(message);
  }

  public HermesGatewayException(String message, Throwable cause) {
    super(message, cause);
  }
}
