package com.aytronn.hermesbridge.service.alexa;

import com.aytronn.hermesbridge.dto.alexa.AlexaTurnRequest;
import com.aytronn.hermesbridge.dto.alexa.AlexaTurnResponse;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class AlexaConversationService {

  private final HermesGatewayClient gatewayClient;

  public AlexaConversationService(HermesGatewayClient gatewayClient) {
    this.gatewayClient = gatewayClient;
  }

  public Mono<AlexaTurnResponse> turn(AlexaTurnRequest request) {
    String conversationId = conversationId(request);
    return gatewayClient.submitTurn(conversationId, conversationId, request.text())
        .map(AlexaTurnResponse::new);
  }

  private static String conversationId(AlexaTurnRequest request) {
    if (request.deviceId() != null && !request.deviceId().isBlank()) {
      return "alexa:" + request.deviceId();
    }
    if (request.sessionId() != null && !request.sessionId().isBlank()) {
      return "alexa-session:" + request.sessionId();
    }
    throw new IllegalArgumentException("deviceId or sessionId must be provided");
  }
}
