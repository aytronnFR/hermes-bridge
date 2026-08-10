package com.aytronn.hermesbridge.controller.alexa;

import com.aytronn.hermesbridge.exception.GlobalExceptionHandler;
import com.aytronn.hermesbridge.exception.HermesGatewayException;
import com.aytronn.hermesbridge.dto.alexa.AlexaTurnResponse;
import com.aytronn.hermesbridge.filter.AlexaApiKeyWebFilter;
import com.aytronn.hermesbridge.service.alexa.AlexaConversationService;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

class AlexaTurnControllerTest {
    private static final AlexaConversationService SERVICE = new AlexaConversationService(new HermesGatewayClient() {
        public Mono<String> submitTurn(String conversationId, String sessionKey, String text) {
            return Mono.just("Hermes répond");
        }
    });

    private final WebTestClient webTestClient = WebTestClient
            .bindToController(new AlexaTurnController(SERVICE))
            .controllerAdvice(new GlobalExceptionHandler())
            .build();

    private final WebTestClient securedWebTestClient = WebTestClient
            .bindToController(new AlexaTurnController(SERVICE))
            .webFilter(new AlexaApiKeyWebFilter("lambda-test-key"))
            .controllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void rejectsAnAlexaTurnWithoutTheBridgeApiKey() {
        securedWebTestClient.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":\"Bonjour Hermes\",\"deviceId\":\"device-1\"}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void rejectsAnAlexaTurnWithTheWrongBridgeApiKey() {
        securedWebTestClient.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer wrong-key")
                .bodyValue("{\"text\":\"Bonjour Hermes\",\"deviceId\":\"device-1\"}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void acceptsAnAlexaTurnWithTheBridgeApiKey() {
        securedWebTestClient.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer lambda-test-key")
                .bodyValue("{\"text\":\"Bonjour Hermes\",\"deviceId\":\"device-1\"}")
                .exchange().expectStatus().isOk();
    }

    @Test
    void returnsHermesResponseForValidTurn() {
        webTestClient.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":\"Bonjour Hermes\",\"deviceId\":\"device-1\"}")
                .exchange().expectStatus().isOk().expectBody().json("{\"text\":\"Hermes répond\"}");
    }

    @Test
    void rejectsBlankText() {
        webTestClient.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":\"   \"}").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void rejectsMalformedJson() {
        webTestClient.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void mapsHermesFailureToBadGateway() {
        AlexaConversationService failingService = new AlexaConversationService(new HermesGatewayClient() {
            public Mono<String> submitTurn(String conversationId, String sessionKey, String text) {
                return Mono.error(new HermesGatewayException("upstream unavailable"));
            }
        });
        WebTestClient client = WebTestClient.bindToController(new AlexaTurnController(failingService))
                .controllerAdvice(new GlobalExceptionHandler()).build();

        client.post().uri("/v1/channels/alexa/turn").contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":\"Bonjour Hermes\",\"deviceId\":\"device-1\"}")
                .exchange().expectStatus().isEqualTo(502)
                .expectBody().jsonPath("$.code").isEqualTo("HERMES_GATEWAY_ERROR");
    }
}
