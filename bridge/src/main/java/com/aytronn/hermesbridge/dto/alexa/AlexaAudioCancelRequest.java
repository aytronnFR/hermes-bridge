package com.aytronn.hermesbridge.dto.alexa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlexaAudioCancelRequest(
    @NotBlank @Size(max = 1024) String playbackToken,
    @NotBlank @Size(max = 512) String userId
) {
}
