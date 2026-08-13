package com.aytronn.hermesbridge.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TtsPropertiesTest {

  @Test
  void defaultsToNinetySecondsForACompleteKokoroSentence() {
    TtsProperties properties = new TtsProperties("http://tts", "kokoro", "ff_siwis", null);

    assertThat(properties.timeoutSeconds()).isEqualTo(90);
  }
}
