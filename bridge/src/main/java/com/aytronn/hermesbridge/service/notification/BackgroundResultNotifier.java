package com.aytronn.hermesbridge.service.notification;

import reactor.core.publisher.Mono;

public interface BackgroundResultNotifier {
  Mono<Void> publish(String result);
}
