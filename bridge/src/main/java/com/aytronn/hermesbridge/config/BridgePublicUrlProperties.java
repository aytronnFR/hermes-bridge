package com.aytronn.hermesbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bridge")
public record BridgePublicUrlProperties(String publicUrl) {
}
