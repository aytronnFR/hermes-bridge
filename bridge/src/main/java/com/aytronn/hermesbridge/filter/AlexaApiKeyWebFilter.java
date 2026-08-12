package com.aytronn.hermesbridge.filter;

import com.aytronn.hermesbridge.config.BridgeApiKeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class AlexaApiKeyWebFilter implements WebFilter {

  private static final String ALEXA_PATH_PREFIX = "/v1/channels/alexa/";
  private static final String AUDIO_STREAM_PATH_PREFIX = "/v1/channels/alexa/audio/streams/";
  private static final String BEARER_PREFIX = "Bearer ";

  private final String apiKey;

  @Autowired
  public AlexaApiKeyWebFilter(BridgeApiKeyProperties properties) {
    this(properties.apiKey());
  }

  public AlexaApiKeyWebFilter(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!path.startsWith(ALEXA_PATH_PREFIX) || path.startsWith(AUDIO_STREAM_PATH_PREFIX)) {
      return chain.filter(exchange);
    }
    if (!hasValidBearerToken(exchange.getRequest().getHeaders())) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
    return chain.filter(exchange);
  }

  private boolean hasValidBearerToken(HttpHeaders headers) {
    String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (apiKey == null || apiKey.isBlank() || authorization == null
        || !authorization.startsWith(BEARER_PREFIX)) {
      return false;
    }
    String providedKey = authorization.substring(BEARER_PREFIX.length());
    return MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8),
        providedKey.getBytes(StandardCharsets.UTF_8));
  }
}
