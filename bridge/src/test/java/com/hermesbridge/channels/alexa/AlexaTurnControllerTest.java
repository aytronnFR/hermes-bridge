package com.hermesbridge.channels.alexa;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.hermesbridge.web.GlobalExceptionHandler;

import static org.springframework.http.MediaType.APPLICATION_JSON;

class AlexaTurnControllerTest {

    private final WebTestClient webTestClient = WebTestClient
            .bindToController(new AlexaTurnController())
            .controllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsFixedAcknowledgementForValidTurn() {
        webTestClient.post()
                .uri("/v1/channels/alexa/turn")
                .contentType(APPLICATION_JSON)
                .bodyValue("""
                        {
                          "text": "Bonjour Hermes",
                          "deviceId": "device-1",
                          "sessionId": "session-1",
                          "requestId": "request-1"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {"text":"Bien reçu chef"}
                        """);
    }

    @Test
    void rejectsBlankText() {
        webTestClient.post()
                .uri("/v1/channels/alexa/turn")
                .contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":\"   \"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.path").isEqualTo("/v1/channels/alexa/turn");
    }

    @Test
    void rejectsMalformedJson() {
        webTestClient.post()
                .uri("/v1/channels/alexa/turn")
                .contentType(APPLICATION_JSON)
                .bodyValue("{\"text\":")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("Request body is invalid");
    }
}
