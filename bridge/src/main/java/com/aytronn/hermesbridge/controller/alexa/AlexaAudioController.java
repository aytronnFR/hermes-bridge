package com.aytronn.hermesbridge.controller.alexa;

import com.aytronn.hermesbridge.dto.alexa.AlexaAudioCancelRequest;
import com.aytronn.hermesbridge.dto.alexa.AlexaAudioJobRequest;
import com.aytronn.hermesbridge.dto.alexa.AlexaAudioJobResponse;
import com.aytronn.hermesbridge.dto.alexa.AlexaAudioLatestRequest;
import com.aytronn.hermesbridge.config.BridgePublicUrlProperties;
import com.aytronn.hermesbridge.service.audio.AudioJob;
import com.aytronn.hermesbridge.service.audio.AudioJobService;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/channels/alexa/audio")
@Slf4j
public class AlexaAudioController {

  private final AudioJobService jobs;
  private final BridgePublicUrlProperties publicUrlProperties;

  public AlexaAudioController(AudioJobService jobs, BridgePublicUrlProperties publicUrlProperties) {
    this.jobs = jobs;
    this.publicUrlProperties = publicUrlProperties;
  }

  @PostMapping("/jobs")
  public Mono<AlexaAudioJobResponse> create(@Valid @RequestBody AlexaAudioJobRequest request,
      ServerWebExchange exchange) {
    AudioJob job = jobs.create(request.userId(), request.deviceId(), request.text());
    return Mono.just(response(job, exchange));
  }

  @PostMapping("/latest")
  public Mono<AlexaAudioJobResponse> latest(@Valid @RequestBody AlexaAudioLatestRequest request,
      ServerWebExchange exchange) {
    AudioJob job = jobs.createLatest(request.userId(), request.deviceId());
    return Mono.just(response(job, exchange));
  }

  private AlexaAudioJobResponse response(AudioJob job, ServerWebExchange exchange) {
    String capability = encode(job.id(), job.token());
    URI base = publicBaseUrl(exchange);
    String streamUrl = base.getScheme() + "://" + base.getAuthority()
        + "/v1/channels/alexa/audio/streams/" + job.id() + "?token=" + job.token();
    log.info("audio_job_response_created jobId={} scheme={} authority={}", job.id(), base.getScheme(), base.getAuthority());
    return new AlexaAudioJobResponse(job.id(), streamUrl, capability, job.backgroundRequested());
  }

  private URI publicBaseUrl(ServerWebExchange exchange) {
    String configured = publicUrlProperties.publicUrl();
    if (configured != null && !configured.isBlank()) {
      return URI.create(configured.replaceAll("/+$", ""));
    }
    return exchange.getRequest().getURI();
  }

  @PostMapping("/cancel")
  public Mono<Void> cancel(@Valid @RequestBody AlexaAudioCancelRequest request) {
    String[] parts = decode(request.playbackToken());
    boolean cancelled = parts == null ? false : jobs.cancel(parts[0], parts[1], request.userId());
    return cancelled ? Mono.empty() : Mono.error(new com.aytronn.hermesbridge.service.audio.AudioJobNotFoundException());
  }

  @GetMapping(value = "/streams/{jobId}", produces = "audio/mpeg")
  public reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer> stream(
      @org.springframework.web.bind.annotation.PathVariable String jobId,
      @RequestParam String token,
      ServerWebExchange exchange) {
    exchange.getResponse().getHeaders().setContentType(MediaType.valueOf("audio/mpeg"));
    exchange.getResponse().getHeaders().setCacheControl("no-store");
    exchange.getResponse().getHeaders().set("Referrer-Policy", "no-referrer");
    return jobs.openStream(jobId, token).map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes));
  }

  private static String encode(String id, String token) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(
        (id + ":" + token).getBytes(StandardCharsets.UTF_8));
  }

  private static String[] decode(String capability) {
    try {
      String value = new String(Base64.getUrlDecoder().decode(capability), StandardCharsets.UTF_8);
      String[] parts = value.split(":", 2);
      return parts.length == 2 ? parts : null;
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
