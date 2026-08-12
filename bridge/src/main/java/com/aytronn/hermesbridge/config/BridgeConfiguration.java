package com.aytronn.hermesbridge.config;

import com.aytronn.hermesbridge.service.alexa.AlexaConversationService;
import com.aytronn.hermesbridge.service.audio.AudioJobService;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import com.aytronn.hermesbridge.service.notification.BackgroundResultNotifier;
import com.aytronn.hermesbridge.service.tts.TtsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({BridgeApiKeyProperties.class, BridgePublicUrlProperties.class,
    HermesGatewayProperties.class, TtsProperties.class, DiscordWebhookProperties.class})
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
  AudioJobService audioJobService(HermesGatewayClient gatewayClient, TtsClient ttsClient,
      BackgroundResultNotifier backgroundResultNotifier) {
    return new AudioJobService(Clock.systemUTC(), Duration.ofMinutes(10), gatewayClient, ttsClient,
        backgroundResultNotifier);
  }

  @Bean
  AlexaConversationService alexaConversationService(HermesGatewayClient client) {
    return new AlexaConversationService(client);
  }
}
