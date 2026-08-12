package com.aytronn.hermesbridge.dto.alexa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlexaAudioJobRequest(
    @NotBlank @Size(max = 8000) String text,
    @NotBlank @Size(max = 512) String userId,
    @NotBlank @Size(max = 512) String deviceId
) {
}
