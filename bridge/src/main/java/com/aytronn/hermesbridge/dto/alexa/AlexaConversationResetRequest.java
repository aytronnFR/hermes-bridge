package com.aytronn.hermesbridge.dto.alexa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlexaConversationResetRequest(
    @NotBlank @Size(max = 512) String userId,
    @NotBlank @Size(max = 512) String deviceId
) { }
