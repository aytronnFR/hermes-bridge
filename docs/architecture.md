# Architecture

## Current MVP

```text
Alexa Custom Skill
  -> versioned Node.js Lambda adapter
  -> POST /v1/channels/alexa/turn
  -> Spring Boot WebFlux bridge
  -> fixed response: Bien reçu chef
```

The bridge is intentionally independent from Hermes Core during this first integration test. It owns the public channel contract; future iterations will add authenticated routing to a Hermes Gateway and persistent channel/device conversation mappings.

## Logging

The Java application uses SLF4J with Logback and emits one JSON event per line to stdout. Loki/Grafana should collect container stdout. Request text and credentials are never logged.

