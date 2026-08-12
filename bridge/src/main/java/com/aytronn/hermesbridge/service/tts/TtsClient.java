package com.aytronn.hermesbridge.service.tts;

import reactor.core.publisher.Mono;

public interface TtsClient {
  Mono<byte[]> synthesize(String text);
}
