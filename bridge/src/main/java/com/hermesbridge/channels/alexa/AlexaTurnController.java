package com.hermesbridge.channels.alexa;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/channels/alexa")
public class AlexaTurnController {

    @PostMapping(value = "/turn", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AlexaTurnResponse> turn(@Valid @RequestBody AlexaTurnRequest request) {
        return Mono.defer(() -> {
            long startedAt = System.nanoTime();
            try (MDC.MDCCloseable channel = MDC.putCloseable("channel", "alexa");
                 MDC.MDCCloseable requestId = MDC.putCloseable("requestId", safe(request.requestId()));
                 MDC.MDCCloseable deviceId = MDC.putCloseable("deviceId", safe(request.deviceId()));
                 MDC.MDCCloseable sessionId = MDC.putCloseable("sessionId", safe(request.sessionId()))) {
                log.info("Alexa turn received");
                AlexaTurnResponse response = new AlexaTurnResponse("Bien reçu chef");
                log.atInfo()
                        .addKeyValue("status", 200)
                        .addKeyValue("durationMs", (System.nanoTime() - startedAt) / 1_000_000)
                        .log("Alexa turn completed");
                return Mono.just(response);
            }
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
