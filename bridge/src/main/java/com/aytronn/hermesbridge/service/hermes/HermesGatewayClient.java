package com.aytronn.hermesbridge.service.hermes;

import reactor.core.publisher.Mono;

public interface HermesGatewayClient {

  Mono<String> submitTurn(String conversationId, String sessionKey, String text);
}
