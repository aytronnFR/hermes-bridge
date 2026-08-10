# Architecture

The backend root package is `com.aytronn.hermesbridge` and uses singular conventional packages: `config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`, and `util`.

Alexa turns are forwarded to `https://hermes-gateway-api.aytronn.com/v1/responses` using the Hermes profile model `alexa`. `AlexaConversationService` derives the named Hermes conversation and long-term memory session key `alexa:<deviceId>`. Hermes persists the response chain, so the same Alexa reuses its conversation after a bridge restart.

## Current MVP

```text
Alexa Custom Skill
  -> versioned Node.js Lambda adapter
  -> POST /v1/channels/alexa/turn
  -> Spring Boot WebFlux bridge
  -> authenticated Hermes Agent Responses API
  -> Hermes profile alexa
  -> assistant response
```

The bridge owns the public channel contract and is independent from Hermes Core internals. Hermes Agent owns the authenticated API and conversation persistence; future iterations will add user-selected gateway routing and a configuration dashboard.

## Logging

The Java application uses SLF4J with Logback and emits one JSON event per line to stdout. Loki/Grafana should collect container stdout. Request text and credentials are never logged.

