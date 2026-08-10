# Hermes Bridge

Hermes Bridge is an open-source channel adapter for [Hermes Agent](https://github.com/NousResearch/hermes-agent).

Its purpose is to make Hermes reachable from voice assistants and custom devices through small, independently deployable channel adapters. The bridge is designed to keep public channel concerns — HTTP contracts, provider-specific payloads, device identity, authentication, and conversation routing — outside Hermes Core.

The first integration is an Alexa Custom Skill. Google Home and custom-device channels can be added later without changing the repository's overall boundary.

## Current status

This repository currently contains a deliberately small end-to-end proof of transport:

```text
Alexa Custom Skill
    -> versioned Node.js Lambda adapter
    -> Spring WebFlux bridge
    -> fixed response: "Bien reçu chef"
```

The bridge exposes:

```http
POST /v1/channels/alexa/turn
GET  /actuator/health
```

For now, it does not call Hermes Core, persist conversations, authenticate users, link Amazon accounts, or implement the dashboard. Those are planned follow-up capabilities.

## Why this repository exists

Hermes Agent already provides the agent runtime and its own communication surfaces. Hermes Bridge focuses on the boundary between external voice/device platforms and Hermes:

- normalize provider-specific requests into a text turn;
- preserve device and request metadata for future routing;
- return a provider-ready text response;
- keep channel adapters versioned and independently deployable;
- prepare persistent device-to-conversation mapping without coupling it to Hermes Core.

## Repository layout

```text
hermes-bridge/
├── bridge/          # Java 25, Spring Boot WebFlux HTTP service
├── alexa-skill/     # Versioned Alexa model and Node.js Lambda adapter
├── dashboard/       # Reserved for the future configuration UI
├── landing-page/    # Reserved for the future public website
├── docs/             # Architecture, contracts, and implementation notes
└── scripts/          # Local verification helpers
```

## Run locally

Requirements:

- Java 25;
- PowerShell on Windows, or an equivalent shell on other platforms;
- Node.js 20+ for the Alexa adapter tests.

Start the bridge from `bridge/`:

```powershell
.\gradlew.bat bootRun
```

The service starts on `http://localhost:8080`.

Check its health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Send a test turn from the repository root:

```powershell
.\scripts\smoke-test.ps1
```

Expected response:

```json
{"text":"Bien reçu chef"}
```

Run backend tests:

```powershell
cd bridge
.\gradlew.bat clean test
```

## Docker

Build the backend image from the `bridge/` directory:

```powershell
docker build -t hermes-bridge:local .\bridge
```

Run the container:

```powershell
docker run --rm --publish 8080:8080 hermes-bridge:local
```

The image uses Java 25, runs as a non-root user, exposes port `8080`, and
provides a container health check through `/actuator/health`. Test the running
container from the repository root with:

```powershell
.\scripts\smoke-test.ps1
```

For a registry deployment, tag the image with the registry name and push it
from your infrastructure pipeline. The repository CI only builds the image;
it does not publish it.

Run Alexa adapter tests:

```powershell
cd alexa-skill\lambda
npm install
npm test
npm run check
```

## Alexa development

The Alexa Skill is stored under `alexa-skill/` using the ASK CLI project layout. Its French invocation name is `hermes`, and the Lambda forwards the recognized text to:

```http
POST ${BRIDGE_URL}/v1/channels/alexa/turn
```

Set `BRIDGE_URL` in the Lambda environment to the public HTTPS base URL of the bridge. The current Alexa-hosted workflow is synchronized through the Alexa Developer Console's Git workflow. The package also retains ASK CLI metadata for future AWS-hosted deployment.

Example utterances:

```text
Alexa, ouvre Hermes
Alexa, envoie bonjour
```

Expected spoken response:

```text
Bien reçu chef
```

See [docs/alexa-skill.md](docs/alexa-skill.md) for the detailed setup and [docs/architecture.md](docs/architecture.md) for the current integration boundary.

## Logging

The Java service uses SLF4J with Logback and emits one JSON event per line to stdout. This is intended for container log collection by Loki/Grafana. Request text, credentials, and other sensitive payloads are not logged.

## Roadmap

- Connect the bridge to a Hermes Gateway;
- persist one Hermes conversation per physical voice device;
- add authenticated gateway configuration;
- add Alexa account linking and a device configuration dashboard;
- add Google Home and custom-device adapters;
- add deployment manifests and production observability.

## Scope and safety

Hermes Bridge is a transport and routing boundary, not a replacement for Hermes Agent. It must not receive Docker access, SSH keys, Hermes profile secrets, or administrative credentials. Production deployments must add authentication, rate limiting, request-size limits, HTTPS, and explicit authorization for agent actions.
