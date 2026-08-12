package com.aytronn.hermesbridge.service.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class HermesStreamDirectiveParserTest {

  @Test
  void emitsProgressAndBackgroundMarkersSplitAcrossGatewayDeltas() {
    List<HermesStreamEvent> events = new HermesStreamDirectiveParser()
        .parse(Flux.just("Je cherche. [[prog", "ress:Je consulte les donnees]]", "[[back", "ground]]Resultat."))
        .collectList().block();

    assertThat(events).containsExactly(
        new HermesStreamEvent.Text("Je cherche. "),
        new HermesStreamEvent.Progress("Je consulte les donnees"),
        HermesStreamEvent.Background.INSTANCE,
        new HermesStreamEvent.Text("Resultat."));
  }

  @Test
  void preservesUnknownMarkersAsFinalText() {
    List<HermesStreamEvent> events = new HermesStreamDirectiveParser()
        .parse(Flux.just("Reponse [[unknown]] finale"))
        .collectList().block();

    assertThat(events).containsExactly(new HermesStreamEvent.Text("Reponse [[unknown]] finale"));
  }
}
