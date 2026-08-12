package com.aytronn.hermesbridge.service.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import com.aytronn.hermesbridge.service.tts.TtsClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class AudioJobServiceTest {

    private final AudioJobService service = new AudioJobService(
            Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC),
            Duration.ofMinutes(10));

    @Test
    void issuesAnOpaqueStreamCapabilityBoundToTheAlexaOwner() {
        AudioJob job = service.create("alexa-user", "alexa-device", "Quel temps fait-il ?");

        assertThat(job.id()).doesNotContain("alexa-user", "alexa-device");
        assertThat(job.token()).doesNotContain("alexa-user", "alexa-device");
        assertThat(service.resolve(job.id(), job.token())).isSameAs(job);
    }

    @Test
    void rejectsUnknownOrInvalidStreamCapabilities() {
        AudioJob job = service.create("alexa-user", "alexa-device", "Bonjour");

        assertThatThrownBy(() -> service.resolve(job.id(), "wrong-token"))
                .isInstanceOf(AudioJobNotFoundException.class);
        assertThatThrownBy(() -> service.resolve("wrong-job", job.token()))
                .isInstanceOf(AudioJobNotFoundException.class);
    }

    @Test
    void invalidTokenDoesNotRevokeTheValidCapability() {
        AudioJob job = service.create("alexa-user", "alexa-device", "Bonjour");

        assertThatThrownBy(() -> service.resolve(job.id(), "wrong-token"))
                .isInstanceOf(AudioJobNotFoundException.class);

        assertThat(service.resolve(job.id(), job.token())).isSameAs(job);
    }

    @Test
    void onlyTheOwningAlexaUserCanCancelAJob() {
        AudioJob job = service.create("alexa-user", "alexa-device", "Bonjour");

        assertThat(service.cancel(job.id(), job.token(), "another-user")).isFalse();
        assertThat(service.cancel(job.id(), job.token(), "alexa-user")).isTrue();
        assertThatThrownBy(() -> service.resolve(job.id(), job.token()))
                .isInstanceOf(AudioJobNotFoundException.class);
    }

    @Test
    void preservesSynthesizedAudioWhenHermesCompletesBeforeAlexaSubscribes() {
        HermesGatewayClient gateway = new HermesGatewayClient() {
            @Override
            public Mono<String> submitTurn(String conversationId, String sessionKey, String text) {
                return Mono.empty();
            }

            @Override
            public Flux<String> streamTurn(String conversationId, String sessionKey, String text) {
                return Flux.just("Bonjour.");
            }
        };
        TtsClient tts = text -> Mono.just(new byte[] {1, 2, 3});
        AudioJobService streamingService = new AudioJobService(
                Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(10), gateway, tts);
        AudioJob job = streamingService.create("alexa-user", "alexa-device", "Bonjour");

        assertThat(streamingService.openStream(job.id(), job.token()).collectList().block())
                .contains(new byte[] {1, 2, 3});
    }
}
