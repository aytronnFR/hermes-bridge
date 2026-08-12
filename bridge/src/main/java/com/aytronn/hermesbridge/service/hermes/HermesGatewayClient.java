package com.aytronn.hermesbridge.service.hermes;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface HermesGatewayClient {

  Mono<String> submitTurn(String conversationId, String sessionKey, String text);

  default Flux<String> streamTurn(String conversationId, String sessionKey, String text) {
    return Flux.error(new UnsupportedOperationException("Streaming is not configured"));
  }
}
