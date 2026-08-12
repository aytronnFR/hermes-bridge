package com.aytronn.hermesbridge.service.audio;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.aytronn.hermesbridge.service.hermes.HermesGatewayClient;
import com.aytronn.hermesbridge.service.notification.BackgroundResultNotifier;
import com.aytronn.hermesbridge.service.tts.TtsClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class AudioJobService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final java.util.regex.Pattern BACKGROUND_REQUEST = java.util.regex.Pattern.compile(
      "(?iu)\\ben[ -]arri[eè]re[ -]plan\\b");

  private final Clock clock;
  private final Duration lifetime;
  private final HermesGatewayClient gatewayClient;
  private final TtsClient ttsClient;
  private final BackgroundResultNotifier backgroundResultNotifier;
  private final Map<String, AudioJob> jobs = new ConcurrentHashMap<>();

  public AudioJobService(Clock clock, Duration lifetime) {
    this(clock, lifetime, null, null, result -> Mono.empty());
  }

  public AudioJobService(Clock clock, Duration lifetime, HermesGatewayClient gatewayClient, TtsClient ttsClient) {
    this(clock, lifetime, gatewayClient, ttsClient, result -> Mono.empty());
  }

  public AudioJobService(Clock clock, Duration lifetime, HermesGatewayClient gatewayClient, TtsClient ttsClient,
      BackgroundResultNotifier backgroundResultNotifier) {
    this.clock = clock;
    this.lifetime = lifetime;
    this.gatewayClient = gatewayClient;
    this.ttsClient = ttsClient;
    this.backgroundResultNotifier = backgroundResultNotifier;
  }

  public AudioJob create(String userId, String deviceId, String text) {
    evictExpired();
    String id = UUID.randomUUID().toString();
    byte[] tokenBytes = new byte[32];
    RANDOM.nextBytes(tokenBytes);
    AudioJob job = new AudioJob(
        id,
        Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes),
        userId,
        deviceId,
        text,
        clock.instant().plus(lifetime),
        BACKGROUND_REQUEST.matcher(text).find()
    );
    jobs.put(id, job);
    log.info("audio_job_created jobId={}", job.id());
    return job;
  }

  public AudioJob resolve(String id, String token) {
    AudioJob job = jobs.get(id);
    if (job == null) {
      throw new AudioJobNotFoundException();
    }
    if (job.expiresAt().isBefore(clock.instant())) {
      jobs.remove(id, job);
      job.cancel();
      throw new AudioJobNotFoundException();
    }
    if (!constantTimeEquals(job.token(), token)) throw new AudioJobNotFoundException();
    return job;
  }

  public boolean cancel(String id, String token, String userId) {
    AudioJob job;
    try {
      job = resolve(id, token);
    } catch (AudioJobNotFoundException ignored) {
      return false;
    }
    if (!constantTimeEquals(job.ownerUserId(), userId)) {
      return false;
    }
    boolean removed = jobs.remove(id, job);
    if (removed) {
      log.info("audio_job_cancelled jobId={}", job.id());
      job.cancel();
    }
    return removed;
  }

  public Flux<byte[]> openStream(String id, String token) {
    AudioJob job = resolve(id, token);
    if (gatewayClient == null || ttsClient == null) throw new IllegalStateException("Audio streaming is not configured");
    if (job.markStarted()) {
      log.info("audio_stream_opened jobId={}", job.id());
      job.upstream(new HermesStreamDirectiveParser().parse(
              gatewayClient.streamTurn("alexa:" + job.ownerDeviceId(), "alexa:" + job.ownerDeviceId(), job.text()))
          .concatMap(event -> handleEvent(job, event))
          .concatWith(Mono.defer(() -> publishBackgroundResult(job)))
          .doOnNext(bytes -> {
            job.speechStarted().tryEmitEmpty();
            log.info("audio_tts_chunk_emitted jobId={} bytes={}", job.id(), bytes.length);
            job.audio().tryEmitNext(bytes);
          })
          .doOnError(error -> {
            log.warn("audio_stream_failed jobId={} errorType={}", job.id(), error.getClass().getSimpleName());
            job.audio().tryEmitComplete();
          })
          .doFinally(signal -> {
            log.info("audio_stream_completed jobId={} signal={}", job.id(), signal);
            job.audio().tryEmitComplete();
            job.completed().tryEmitEmpty();
          })
          .subscribe());
    }
    Flux<byte[]> waitingMusic = Flux.interval(Duration.ZERO, Duration.ofMillis(350))
        .map(ignored -> SilenceMp3.SEGMENT)
        .takeUntilOther(Mono.firstWithSignal(job.speechStarted().asMono(), job.completed().asMono()));
    return Flux.merge(waitingMusic, job.audio().asFlux());
  }

  private Flux<byte[]> handleEvent(AudioJob job, HermesStreamEvent event) {
    if (event instanceof HermesStreamEvent.Background) {
      job.requestBackground();
      log.info("audio_job_moved_to_background jobId={}", job.id());
      return Flux.empty();
    }
    if (event instanceof HermesStreamEvent.Progress progress) {
      return job.backgroundRequested() ? Flux.empty() : ttsClient.synthesize(progress.value()).flux();
    }
    HermesStreamEvent.Text text = (HermesStreamEvent.Text) event;
    job.appendFinalResult(text.value());
    return job.backgroundRequested() ? Flux.empty() : ttsClient.synthesize(text.value()).flux();
  }

  private Mono<byte[]> publishBackgroundResult(AudioJob job) {
    if (!job.backgroundRequested() || job.finalResult().isBlank()) return Mono.empty();
    return backgroundResultNotifier.publish(job.finalResult()).then(Mono.empty());
  }

  private void evictExpired() {
    Instant now = clock.instant();
    jobs.values().removeIf(job -> job.expiresAt().isBefore(now));
  }

  private static boolean constantTimeEquals(String expected, String supplied) {
    if (supplied == null) {
      return false;
    }
    return java.security.MessageDigest.isEqual(
        expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    );
  }
}
