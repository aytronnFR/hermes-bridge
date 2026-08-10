package com.hermesbridge.channels.alexa;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/channels/alexa")
public class AlexaTurnController {

    @PostMapping(value = "/turn", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AlexaTurnResponse> turn(@Valid @RequestBody AlexaTurnRequest request) {
        return Mono.just(new AlexaTurnResponse("Bien reçu chef"));
    }
}

