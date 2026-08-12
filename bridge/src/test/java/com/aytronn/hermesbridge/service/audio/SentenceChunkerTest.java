package com.aytronn.hermesbridge.service.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class SentenceChunkerTest {

  @Test
  void emitsCompleteSentencesAndTheRemainingText() {
    List<String> sentences = SentenceChunker.sentences(Flux.just(
        "Bonjour tout ", "le monde. Voici une ", "seconde phrase ! Et la fin"))
        .collectList()
        .block();

    assertThat(sentences).containsExactly("Bonjour tout le monde.", "Voici une seconde phrase !", "Et la fin");
  }
}
