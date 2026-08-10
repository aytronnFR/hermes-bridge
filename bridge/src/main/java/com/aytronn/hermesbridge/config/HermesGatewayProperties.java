package com.aytronn.hermesbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hermes.gateway")
public record HermesGatewayProperties(String baseUrl, String apiKey, String model) {

}
