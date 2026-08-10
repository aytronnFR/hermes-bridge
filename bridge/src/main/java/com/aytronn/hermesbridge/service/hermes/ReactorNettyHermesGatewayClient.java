package com.aytronn.hermesbridge.service.hermes;

import com.aytronn.hermesbridge.config.HermesGatewayProperties;
import com.aytronn.hermesbridge.exception.HermesGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ReactorNettyHermesGatewayClient implements HermesGatewayClient {

  private static final String SESSION_KEY_HEADER = "X-Hermes-Session-Key";

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final HermesGatewayProperties properties;

  public ReactorNettyHermesGatewayClient(WebClient.Builder builder, ObjectMapper objectMapper,
      HermesGatewayProperties properties) {
    this.webClient = builder.baseUrl(properties.baseUrl()).build();
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public Mono<String> submitTurn(String conversationId, String sessionKey, String text) {
    ObjectNode body = objectMapper.createObjectNode()
        .put("model", required(properties.model(), "HERMES_GATEWAY_MODEL"))
        .put("input", text)
        .put("conversation", conversationId)
        .put("store", true);
    String requestBody = writeJson(body);

    return webClient.post()
        .uri("/v1/responses")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(headers -> {
          headers.setBearerAuth(required(properties.apiKey(), "HERMES_GATEWAY_API_KEY"));
          headers.set(SESSION_KEY_HEADER, sessionKey);
        })
        .bodyValue(requestBody)
        .exchangeToMono(response -> {
          if (!response.statusCode().is2xxSuccessful()) {
            return Mono.error(new HermesGatewayException("Hermes Gateway API request failed"));
          }
          return response.bodyToMono(String.class).map(this::readTree);
        })
        .map(this::extractOutputText)
        .timeout(Duration.ofSeconds(30))
        .onErrorMap(error -> error instanceof HermesGatewayException
            ? error : new HermesGatewayException("Hermes Gateway API request failed", error));
  }

  private String extractOutputText(JsonNode response) {
    for (JsonNode output : response.path("output")) {
      if (!"message".equals(output.path("type").asText())) {
        continue;
      }
      for (JsonNode content : output.path("content")) {
        String text = content.path("text").asText("");
        if ("output_text".equals(content.path("type").asText()) && !text.isBlank()) {
          return text;
        }
      }
    }
    throw new HermesGatewayException("Hermes Gateway API response contains no output text");
  }

  private JsonNode readTree(String responseBody) {
    try {
      return objectMapper.readTree(responseBody);
    } catch (Exception error) {
      throw new HermesGatewayException("Hermes Gateway API returned invalid JSON", error);
    }
  }

  private String writeJson(JsonNode body) {
    try {
      return objectMapper.writeValueAsString(body);
    } catch (Exception error) {
      throw new HermesGatewayException("Could not serialize Hermes Gateway API request", error);
    }
  }

  private static String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new HermesGatewayException(name + " is not configured");
    }
    return value;
  }
}
