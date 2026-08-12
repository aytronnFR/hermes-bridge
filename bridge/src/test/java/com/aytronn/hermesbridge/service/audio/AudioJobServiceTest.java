package com.aytronn.hermesbridge.service.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
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
    void marksAnExplicitFrenchBackgroundRequestBeforeTheAudioStreamStarts() {
        AudioJob job = service.create("alexa-user", "alexa-device", "Fais le rapport en arrière-plan");

        assertThat(job.backgroundRequested()).isTrue();
    }

    @Test
    void sendsOnlyTheFinalTextToTheBackgroundNotifier() {
        AtomicReference<String> delivered = new AtomicReference<>();
        HermesGatewayClient gateway = new HermesGatewayClient() {
            @Override public Mono<String> submitTurn(String conversationId, String sessionKey, String text) { return Mono.empty(); }
            @Override public Flux<String> streamTurn(String conversationId, String sessionKey, String text) {
                return Flux.just("Rapport termine.");
            }
        };
        TtsClient tts = text -> Mono.error(new AssertionError("background work must not call TTS"));
        AudioJobService streamingService = new AudioJobService(
            Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(10), gateway, tts,
            result -> { delivered.set(result); return Mono.empty(); });
        AudioJob job = streamingService.create("alexa-user", "alexa-device", "Fais le rapport en arriere-plan");

        streamingService.openStream(job.id(), job.token()).collectList().block();

        assertThat(delivered.get()).isEqualTo("Rapport termine.");
    }

    @Test
    void synthesizesGatewayFragmentsAsOneCompleteSentence() {
        CopyOnWriteArrayList<String> synthesized = new CopyOnWriteArrayList<>();
        HermesGatewayClient gateway = new HermesGatewayClient() {
            @Override public Mono<String> submitTurn(String conversationId, String sessionKey, String text) { return Mono.empty(); }
            @Override public Flux<String> streamTurn(String conversationId, String sessionKey, String text) {
                return Flux.just("Bonjour, ", "comment allez", "-vous ?");
            }
        };
        TtsClient tts = text -> {
            synthesized.add(text);
            return Mono.just(new byte[] {1, 2, 3});
        };
        AudioJobService streamingService = new AudioJobService(
            Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(10), gateway, tts);
        AudioJob job = streamingService.create("alexa-user", "alexa-device", "Bonjour");

        streamingService.openStream(job.id(), job.token()).collectList().block();

        assertThat(synthesized).containsExactly("Bonjour, comment allez-vous ?");
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
        TtsClient tts = text -> Mono.delay(Duration.ofMillis(50)).thenReturn(new byte[] {1, 2, 3});
        AudioJobService streamingService = new AudioJobService(
                Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(10), gateway, tts);
        AudioJob job = streamingService.create("alexa-user", "alexa-device", "Bonjour");

        assertThat(streamingService.openStream(job.id(), job.token()).collectList().block())
                .anySatisfy(chunk -> assertThat(chunk).isEqualTo(SilenceMp3.SEGMENT))
                .anySatisfy(chunk -> assertThat(chunk).isEqualTo(new byte[] {1, 2, 3}));
    }

    @Test
    void replaysAudioWhenAlexaReconnectsToTheSameStream() {
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
                .containsExactly(new byte[] {1, 2, 3});
        assertThat(streamingService.openStream(job.id(), job.token()).collectList().block())
                .containsExactly(new byte[] {1, 2, 3});
    }
}
