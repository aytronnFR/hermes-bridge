package com.aytronn.hermesbridge.service.alexa;

import com.aytronn.hermesbridge.dto.alexa.AlexaTurnRequest;
import com.aytronn.hermesbridge.dto.alexa.AlexaTurnResponse;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class AlexaConversationService {

  private final HermesGatewayClient gatewayClient;
  private final Map<String, String> conversationIds = new ConcurrentHashMap<>();

  public AlexaConversationService(HermesGatewayClient gatewayClient) {
    this.gatewayClient = gatewayClient;
  }

  public Mono<AlexaTurnResponse> turn(AlexaTurnRequest request) {
    String conversationId = conversationId(request);
    return gatewayClient.submitTurn(conversationId, conversationId, request.text())
        .map(AlexaTurnResponse::new);
  }

  public String conversationId(String userId, String deviceId) {
    if (deviceId == null || deviceId.isBlank()) throw new IllegalArgumentException("deviceId must be provided");
    if (userId == null || userId.isBlank()) return "alexa:" + deviceId;
    return conversationIds.getOrDefault(ownerKey(userId, deviceId), "alexa:" + deviceId);
  }

  public String resetConversation(String userId, String deviceId) {
    if (userId == null || userId.isBlank() || deviceId == null || deviceId.isBlank()) {
      throw new IllegalArgumentException("userId and deviceId must be provided");
    }
    String conversationId = "alexa:" + UUID.randomUUID();
    conversationIds.put(ownerKey(userId, deviceId), conversationId);
    log.info("alexa_conversation_reset deviceId={}", deviceId);
    return conversationId;
  }

  private String conversationId(AlexaTurnRequest request) {
    if (request.deviceId() != null && !request.deviceId().isBlank()) {
      return conversationId(null, request.deviceId());
    }
    if (request.sessionId() != null && !request.sessionId().isBlank()) {
      return "alexa-session:" + request.sessionId();
    }
    throw new IllegalArgumentException("deviceId or sessionId must be provided");
  }

  private static String ownerKey(String userId, String deviceId) {
    return userId + "\u0000" + deviceId;
  }
}
