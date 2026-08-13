package com.aytronn.hermesbridge.controller.alexa;

import com.aytronn.hermesbridge.dto.alexa.AlexaTurnRequest;
import com.aytronn.hermesbridge.dto.alexa.AlexaTurnResponse;
import com.aytronn.hermesbridge.dto.alexa.AlexaConversationResetRequest;
import com.aytronn.hermesbridge.service.alexa.AlexaConversationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/channels/alexa")
public class AlexaTurnController {

  private final AlexaConversationService conversationService;

  public AlexaTurnController(AlexaConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @PostMapping(value = "/turn", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<AlexaTurnResponse> turn(@Valid @RequestBody AlexaTurnRequest request) {
    try (MDC.MDCCloseable channel = MDC.putCloseable("channel", "alexa");
        MDC.MDCCloseable requestId = MDC.putCloseable("requestId", safe(request.requestId()));
        MDC.MDCCloseable deviceId = MDC.putCloseable("deviceId", safe(request.deviceId()))) {
      log.info("Alexa turn received");
      return conversationService.turn(request);
    }
  }

  @PostMapping(value = "/conversations/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Void> resetConversation(@Valid @RequestBody AlexaConversationResetRequest request) {
    conversationService.resetConversation(request.userId(), request.deviceId());
    return Mono.empty();
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
