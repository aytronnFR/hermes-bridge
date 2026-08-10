# Hermes Bridge MVP Design

Date: 2026-08-10
Status: proposed for review

## Goal

Create a separate `hermes-bridge` repository that receives a text turn from the first Alexa Skill integration and returns the fixed French response `Bien reçu chef`.

The MVP validates the end-to-end Alexa-to-bridge transport. It does not yet authenticate users, call Hermes Core, persist conversations, connect Amazon accounts, connect Google Home, or provide a working dashboard.

## Repository structure

```text
hermes-bridge/
├── bridge/
│   └── Spring Boot WebFlux service, Gradle Kotlin DSL build, Logback configuration, and tests
├── alexa-skill/
│   ├── skill-package/
│   │   ├── skill.json
│   │   └── interactionModels/custom/fr-FR.json
│   ├── lambda/
│   │   ├── index.js
│   │   └── package.json
│   └── ask-resources.json
├── dashboard/
├── landing-page/
├── docs/
│   ├── architecture.md
│   ├── alexa-skill.md
│   └── superpowers/specs/2026-08-10-hermes-bridge-mvp-design.md
├── README.md
└── .gitignore
```

`dashboard/` and `landing-page/` are reserved for later work and contain only scope documentation in this MVP.

## Backend

- Java 21.
- Spring Boot 4.1.x.
- Spring WebFlux with Reactor Netty.
- Gradle Kotlin DSL with the Gradle Wrapper (`build.gradle.kts`, `settings.gradle.kts`).
- Dependency versions managed centrally when useful through `gradle/libs.versions.toml`.
- One endpoint: `POST /v1/channels/alexa/turn`.
- One operational endpoint: `GET /actuator/health`.
- No database, Hermes call, OAuth, or external identity provider.
- Application code uses the SLF4J API with Logback as the explicit logging implementation.
- The JSON encoder is provided by `logstash-logback-encoder`, configured without application-managed log files.
- A versioned `src/main/resources/logback-spring.xml` configuration emits one structured JSON event per stdout line for Loki/Grafana ingestion.
- Logs include stable service metadata and safe correlation fields such as `channel`, `requestId`, and `deviceId` when present.
- Request text, authorization material, and other sensitive payloads are not logged.

The Alexa endpoint accepts a JSON object with `text` and optional `deviceId`, `sessionId`, and `requestId` strings. A valid request returns HTTP 200:

```json
{
  "text": "Bien reçu chef"
}
```

Malformed JSON or invalid fields return HTTP 400 using a documented error shape. The service logs request metadata without secrets or full sensitive content.

The endpoint is intentionally suitable for development testing only. Production exposure must add authentication, rate limiting, request-size limits, and HTTPS enforcement.

The Logback configuration is documented and verified as one JSON object per line. Grafana/Loki is expected to collect container stdout rather than application-managed log files.

## Alexa Skill

The Alexa project is versioned in the same repository using the ASK CLI package layout. It contains the manifest, French interaction model, Lambda source, package metadata, and deployment configuration.

The skill will expose a custom interaction that forwards the recognized text and Alexa request metadata to the bridge endpoint. The Lambda reads the bridge response and speaks its `text` value. The bridge URL is configuration, not a hard-coded secret.

The skill documentation covers local payload tests, the bridge URL configuration, ASK CLI setup, deployment to the development stage, and Alexa simulator testing.

## Data flow

```text
User speaks
  -> Alexa custom skill
  -> versioned Lambda
  -> POST /v1/channels/alexa/turn
  -> { "text": "Bien reçu chef" }
  -> Alexa speaks the response
```

## Testing and verification

- Backend unit/web test for the valid Alexa payload and fixed response.
- Backend test for malformed input returning 400.
- Health endpoint verification.
- Alexa Lambda unit test with a mocked bridge response.
- Documented manual Alexa simulator test.

## Deferred scope

- Hermes Gateway integration and conversation persistence.
- Device-to-conversation mapping.
- User accounts and Alexa Account Linking.
- Gateway selection.
- Dashboard and landing page implementation.
- Google Home and custom-device channels.
- Production deployment manifests and public authentication.
