package com.aytronn.hermesbridge.controller.alexa;

import static org.assertj.core.api.Assertions.assertThat;

import com.aytronn.hermesbridge.config.BridgePublicUrlProperties;
import com.aytronn.hermesbridge.dto.alexa.AlexaAudioJobRequest;
import com.aytronn.hermesbridge.dto.alexa.AlexaAudioJobResponse;
import com.aytronn.hermesbridge.service.audio.AudioJobService;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class AlexaAudioControllerTest {

  @Test
  void usesTheConfiguredPublicHttpsUrlInsteadOfTheInternalRequestScheme() {
    AlexaAudioController controller = new AlexaAudioController(
        new AudioJobService(Clock.systemUTC(), Duration.ofMinutes(10)),
        new BridgePublicUrlProperties("https://hermes-bridge-api.aytronn.com/"));
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("http://hermes-bridge.web.svc.cluster.local/v1/channels/alexa/audio/jobs").build());

    AlexaAudioJobResponse response = controller.create(
        new AlexaAudioJobRequest("bonjour", "user", "device"), exchange).block();

    assertThat(response.streamUrl()).startsWith("https://hermes-bridge-api.aytronn.com/v1/channels/alexa/audio/streams/");
  }
}
