package com.aytronn.hermesbridge.config;

import com.aytronn.hermesbridge.service.alexa.AlexaConversationService;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({BridgeApiKeyProperties.class, HermesGatewayProperties.class})
public class BridgeConfiguration {

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  AlexaConversationService alexaConversationService(HermesGatewayClient client) {
    return new AlexaConversationService(client);
  }
}
