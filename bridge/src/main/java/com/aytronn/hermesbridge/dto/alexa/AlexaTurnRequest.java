package com.aytronn.hermesbridge.dto.alexa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlexaTurnRequest(
    @NotBlank(message = "text must not be blank")
    @Size(max = 8000, message = "text must not exceed 8000 characters") String text,
    @Size(max = 512, message = "deviceId must not exceed 512 characters") String deviceId,
    @Size(max = 512, message = "sessionId must not exceed 512 characters") String sessionId,
    @Size(max = 512, message = "requestId must not exceed 512 characters") String requestId
) {

}
