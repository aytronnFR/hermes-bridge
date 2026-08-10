package com.hermesbridge.web;

public record ApiError(String code, String message, String path) {
}

