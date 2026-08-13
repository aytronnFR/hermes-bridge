package com.aytronn.hermesbridge.service.tts;

import com.aytronn.hermesbridge.config.TtsProperties;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ReactorNettyTtsClient implements TtsClient {
  private final WebClient client;
  private final TtsProperties properties;

  public ReactorNettyTtsClient(WebClient.Builder builder, TtsProperties properties) {
    this.client = builder.baseUrl(properties.baseUrl()).build();
    this.properties = properties;
  }

  @Override
  public Mono<byte[]> synthesize(String text) {
    return client.post().uri("/v1/audio/speech")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.valueOf("audio/mpeg"))
        .bodyValue(Map.of("model", properties.model(), "voice", properties.voice(), "input", text,
            "response_format", "mp3"))
        .retrieve().bodyToMono(byte[].class).timeout(Duration.ofSeconds(properties.timeoutSeconds()));
  }
}
