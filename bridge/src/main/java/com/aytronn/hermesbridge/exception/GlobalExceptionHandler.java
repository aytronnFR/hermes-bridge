package com.aytronn.hermesbridge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ApiError> handleValidation(WebExchangeBindException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("VALIDATION_ERROR", "Request validation failed", "/v1/channels/alexa/turn"));
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ApiError> handleMalformedInput(ServerWebInputException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("INVALID_REQUEST", "Request body is invalid", "request"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleInvalidConversation(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("INVALID_CONVERSATION", exception.getMessage(), "/v1/channels/alexa/turn"));
  }

  @ExceptionHandler(HermesGatewayException.class)
  public ResponseEntity<ApiError> handleHermesFailure(HermesGatewayException exception) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ApiError("HERMES_GATEWAY_ERROR", "Hermes Gateway is unavailable", "/v1/channels/alexa/turn"));
  }
}
