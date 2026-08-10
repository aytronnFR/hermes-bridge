package com.aytronn.hermesbridge.repository;

import java.util.function.Supplier;
import reactor.core.publisher.Mono;

public interface HermesConversationRepository {

  Mono<String> findOrCreate(String conversationKey, Supplier<Mono<String>> sessionFactory);
}
