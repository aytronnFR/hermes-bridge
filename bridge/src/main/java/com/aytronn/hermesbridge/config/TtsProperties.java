package com.aytronn.hermesbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tts")
public record TtsProperties(String baseUrl, String model, String voice) {
}
