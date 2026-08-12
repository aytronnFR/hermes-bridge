package com.aytronn.hermesbridge.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BridgePublicUrlPropertiesTest {

  @Test
  void retainsTheExplicitPublicHttpsUrl() {
    BridgePublicUrlProperties properties = new BridgePublicUrlProperties("https://hermes-bridge-api.aytronn.com");

    assertThat(properties.publicUrl()).startsWith("https://");
  }
}
