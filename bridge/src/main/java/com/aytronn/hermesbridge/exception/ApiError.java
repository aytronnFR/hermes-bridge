package com.aytronn.hermesbridge.exception;

public record ApiError(String code, String message, String path) {

}
