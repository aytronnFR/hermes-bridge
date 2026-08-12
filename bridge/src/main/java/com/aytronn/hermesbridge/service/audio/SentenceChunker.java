package com.aytronn.hermesbridge.service.audio;

import reactor.core.publisher.Flux;

final class SentenceChunker {
  private SentenceChunker() { }

  static Flux<String> sentences(Flux<String> deltas) {
    StringBuilder pending = new StringBuilder();
    return deltas.concatMapIterable(delta -> {
      pending.append(delta);
      java.util.List<String> completed = new java.util.ArrayList<>();
      int boundary;
      while ((boundary = boundary(pending)) >= 0) {
        String sentence = pending.substring(0, boundary + 1).trim();
        pending.delete(0, boundary + 1);
        if (!sentence.isBlank()) completed.add(sentence);
      }
      return completed;
    }).concatWith(Flux.defer(() -> {
      String tail = pending.toString().trim();
      return tail.isBlank() ? Flux.empty() : Flux.just(tail);
    }));
  }

  private static int boundary(StringBuilder text) {
    for (int i = 0; i < text.length(); i++) {
      if (".!?".indexOf(text.charAt(i)) >= 0 && (i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)))) return i;
    }
    return -1;
  }
}
