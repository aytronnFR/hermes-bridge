package com.aytronn.hermesbridge.service.hermes;

import static org.assertj.core.api.Assertions.assertThat;

import com.aytronn.hermesbridge.config.HermesGatewayProperties;
import com.aytronn.hermesbridge.exception.HermesGatewayException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ReactorNettyHermesGatewayClientTest {

  @Test
  void sendsAnAuthenticatedResponsesRequestForTheAlexaConversation() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("""
          {"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"Bonjour chef"}]}]}
          """));
      server.start();
      ReactorNettyHermesGatewayClient client = client(server);

      StepVerifier.create(client.submitTurn("alexa:device-1", "alexa:device-1", "bonjour"))
          .expectNext("Bonjour chef")
          .verifyComplete();

      RecordedRequest request = server.takeRequest();
      assertThat(request.getPath()).isEqualTo("/v1/responses");
      assertThat(request.getHeader("Authorization")).isEqualTo("Bearer bridge-test-key");
      assertThat(request.getHeader("X-Hermes-Session-Key")).isEqualTo("alexa:device-1");
      assertThat(request.getBody().readUtf8())
          .isEqualTo("{\"model\":\"alexa\",\"input\":\"bonjour\",\"conversation\":\"alexa:device-1\",\"store\":true}");
    }
  }

  @Test
  void hidesTheApiKeyWhenHermesRejectsTheRequest() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(401));
      server.start();
      ReactorNettyHermesGatewayClient client = client(server);

      StepVerifier.create(client.submitTurn("alexa:device-1", "alexa:device-1", "bonjour"))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(HermesGatewayException.class);
            assertThat(error.getMessage()).doesNotContain("bridge-test-key");
          })
          .verify();
    }
  }

  private static ReactorNettyHermesGatewayClient client(MockWebServer server) {
    HermesGatewayProperties properties = new HermesGatewayProperties(
        server.url("/").toString(), "bridge-test-key", "alexa");
    return new ReactorNettyHermesGatewayClient(WebClient.builder(), new ObjectMapper(), properties);
  }
}
