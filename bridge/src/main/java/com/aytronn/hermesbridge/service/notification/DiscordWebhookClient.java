package com.aytronn.hermesbridge.service.notification;

import com.aytronn.hermesbridge.config.DiscordWebhookProperties;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DiscordWebhookClient implements BackgroundResultNotifier {
  private static final int DISCORD_CONTENT_LIMIT = 2_000;

  private final WebClient webClient;
  private final DiscordWebhookProperties properties;

  public DiscordWebhookClient(WebClient.Builder builder, DiscordWebhookProperties properties) {
    this.webClient = builder.build();
    this.properties = properties;
  }

  @Override
  public Mono<Void> publish(String result) {
    if (properties.url() == null || properties.url().isBlank()) {
      log.warn("background_result_discarded reason=discord_webhook_not_configured");
      return Mono.empty();
    }
    String content = result.length() <= DISCORD_CONTENT_LIMIT ? result
        : result.substring(0, DISCORD_CONTENT_LIMIT - 1) + "…";
    return webClient.post().uri(properties.url())
        .bodyValue(Map.of("content", content))
        .retrieve()
        .toBodilessEntity()
        .then()
        .doOnSuccess(ignored -> log.info("background_result_discord_published"))
        .doOnError(error -> log.warn("background_result_discord_failed errorType={}",
            error.getClass().getSimpleName()))
        .onErrorResume(error -> Mono.empty());
  }
}
