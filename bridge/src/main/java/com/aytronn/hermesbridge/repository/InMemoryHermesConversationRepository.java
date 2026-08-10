package com.aytronn.hermesbridge.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InMemoryHermesConversationRepository implements HermesConversationRepository {

  private final ConcurrentHashMap<String, Mono<String>> sessions = new ConcurrentHashMap<>();

  @Override
  public Mono<String> findOrCreate(String conversationKey, Supplier<Mono<String>> sessionFactory) {
    return sessions.computeIfAbsent(conversationKey, key -> sessionFactory.get()
        .doOnError(error -> sessions.remove(key))
        .cache());
  }
}
