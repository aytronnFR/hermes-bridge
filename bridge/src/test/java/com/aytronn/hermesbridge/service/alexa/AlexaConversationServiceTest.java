package com.aytronn.hermesbridge.service.alexa;

import com.aytronn.hermesbridge.dto.alexa.AlexaTurnRequest;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AlexaConversationServiceTest {

    @Test
    void usesTheAlexaDeviceAsTheHermesConversationAndSessionKey() {
        AtomicReference<String> conversation = new AtomicReference<>();
        AtomicReference<String> sessionKey = new AtomicReference<>();
        HermesGatewayClient client = new HermesGatewayClient() {
            @Override
            public Mono<String> submitTurn(String conversationId, String hermesSessionKey, String text) {
                conversation.set(conversationId);
                sessionKey.set(hermesSessionKey);
                return Mono.just("Bien reçu chef");
            }
        };
        AlexaConversationService service = new AlexaConversationService(client);

        StepVerifier.create(service.turn(new AlexaTurnRequest("bonjour", "device-1", "alexa-1", "request-1")))
                .expectNext(new com.aytronn.hermesbridge.dto.alexa.AlexaTurnResponse("Bien reçu chef"))
                .verifyComplete();

        assertThat(conversation).hasValue("alexa:device-1");
        assertThat(sessionKey).hasValue("alexa:device-1");
    }

    @Test
    void createsANewConversationKeyForTheAlexaDeviceAfterReset() {
        AlexaConversationService service = new AlexaConversationService(new HermesGatewayClient() {
            @Override
            public Mono<String> submitTurn(String conversationId, String hermesSessionKey, String text) {
                return Mono.just("Bien reçu chef");
            }
        });

        String previous = service.conversationId("user-1", "device-1");
        String current = service.resetConversation("user-1", "device-1");

        assertThat(previous).isEqualTo("alexa:device-1");
        assertThat(current).startsWith("alexa:").isNotEqualTo(previous);
        assertThat(service.conversationId("user-1", "device-1")).isEqualTo(current);
    }
}
