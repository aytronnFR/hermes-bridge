# Hermes Bridge

The current bridge forwards Alexa turns to the Hermes Agent API of the `alexa` profile through `https://hermes-gateway-api.aytronn.com`.

Backend configuration:

```text
HERMES_BRIDGE_API_KEY=<Lambda-to-Bridge API key>
HERMES_GATEWAY_URL=https://hermes-gateway-api.aytronn.com
HERMES_GATEWAY_API_KEY=<Bridge-to-Hermes API key>
HERMES_GATEWAY_MODEL=alexa
```

The bridge sends `alexa:<deviceId>` as the Hermes named conversation and session key. Hermes retains one conversation per physical Alexa device, including across bridge restarts. Credentials and prompts are never logged.

Hermes Bridge is an open-source channel adapter for [Hermes Agent](https://github.com/NousResearch/hermes-agent).

Its purpose is to make Hermes reachable from voice assistants and custom devices through small, independently deployable channel adapters. The bridge is designed to keep public channel concerns — HTTP contracts, provider-specific payloads, device identity, authentication, and conversation routing — outside Hermes Core.

The first integration is an Alexa Custom Skill. Google Home and custom-device channels can be added later without changing the repository's overall boundary.

## Current status

This repository currently contains a deliberately small end-to-end proof of transport:

```text
Alexa Custom Skill
    -> versioned Node.js Lambda adapter
    -> Spring WebFlux bridge
    -> authenticated Hermes Agent Responses API
    -> Hermes profile `alexa`
```

The bridge exposes:

```http
POST /v1/channels/alexa/turn
POST /v1/channels/alexa/audio/jobs
POST /v1/channels/alexa/audio/cancel
GET  /actuator/health
```

For now, it does not authenticate users, link Amazon accounts, or implement the dashboard. Those are planned follow-up capabilities.

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
{"text":"<Hermes assistant response>"}
```

Run backend tests:

```powershell
cd bridge
.\gradlew.bat clean test
```

## Connect the bridge to Hermes Agent

Hermes Bridge talks to the official Hermes API, not to the Hermes dashboard.
Keep the Hermes API bound to loopback and expose it through an authenticated
HTTPS tunnel or reverse proxy. Do not expose port `8642` directly to the
Internet.

### 1. Generate two distinct keys

Generate one value for Hermes Agent and another for Lambda-to-Bridge
authentication. Store both in a password manager; never commit or paste them
into source code.

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$hermesGatewayKey = "hmb_" + [Convert]::ToHexString($bytes).ToLowerInvariant()
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$bridgeApiKey = "hmb_" + [Convert]::ToHexString($bytes).ToLowerInvariant()
```

### 2. Configure the Hermes profile

On the machine running the `alexa` Hermes profile, add the following to
`/home/debian/.hermes/profiles/alexa/.env` (adapt the home directory and
profile name for your installation):

```dotenv
API_SERVER_ENABLED=true
API_SERVER_HOST=127.0.0.1
API_SERVER_PORT=8642
API_SERVER_KEY=<paste $hermesGatewayKey>
```

Restart that profile gateway, for example:

```bash
systemctl --user restart hermes-gateway-alexa.service
```

### 3. Publish only the local API through the tunnel

Configure the public hostname used by the bridge to target this local origin:

```text
https://hermes-gateway-api.example.com -> http://127.0.0.1:8642
```

Port `9119` is the Hermes dashboard and must not be used as the bridge origin.

### 4. Configure the bridge

Inject the same key into the bridge runtime. In development, PowerShell can
set the variables for the current process:

```powershell
$env:HERMES_BRIDGE_API_KEY = "<paste $bridgeApiKey>"
$env:HERMES_GATEWAY_URL = "https://hermes-gateway-api.example.com"
$env:HERMES_GATEWAY_API_KEY = "<paste $hermesGatewayKey>"
$env:HERMES_GATEWAY_MODEL = "alexa"
cd bridge
.\gradlew.bat bootRun
```

In Docker, Kubernetes, or another deployment environment, inject both
`HERMES_BRIDGE_API_KEY` and `HERMES_GATEWAY_API_KEY` through its secret
mechanism rather than embedding them in an image or a tracked `.env` file.

### 5. Configure the Alexa Lambda

Add these environment variables to the Alexa-hosted Lambda (or the AWS Lambda
function if you self-host it):

```text
BRIDGE_URL=https://bridge.example.com
BRIDGE_API_KEY=<paste $bridgeApiKey>
```

The Lambda sends this value as `Authorization: Bearer ...` for every Alexa turn.
The bridge accepts it only on `/v1/channels/alexa/**`; its health endpoint and
Hermes API credentials remain separate.

### 6. Verify the Hermes API without disclosing the key

The unauthenticated request must return `401`; this proves the public hostname
reaches the Hermes API and that bearer authentication is enforced:

```powershell
curl.exe -sS -o NUL -w "HTTP %{http_code}`n" https://hermes-gateway-api.example.com/v1/models
```

Then send an authenticated request from a trusted shell using the environment
variable, never a literal token in shell history:

```powershell
curl.exe -sS -H "Authorization: Bearer $env:HERMES_GATEWAY_API_KEY" https://hermes-gateway-api.example.com/v1/models
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

On every successful service build on `main`, GitHub Actions publishes a
candidate image to the GitHub Container Registry with the following tags:

```text
ghcr.io/aytronnfr/hermes-bridge:latest
ghcr.io/aytronnfr/hermes-bridge:main-<short-sha>
```

The `latest` tag is only used by the release workflow to resolve the most
recent validated candidate. ArgoCD never consumes `latest`: the release job
reads the image revision label and writes the immutable `main-<short-sha>` tag to
`green-infra`. Infrastructure is updated only when every selected service in
the matrix has passed.

For infrastructure deployments, prefer the immutable `main-<short-sha>` tag:

```powershell
docker pull ghcr.io/aytronnfr/hermes-bridge:main-<short-sha>
```

Pulling the image may require authenticating to `ghcr.io` if the package is
private. Pull requests still build the image, but do not publish it.

Run Alexa adapter tests:

```powershell
cd alexa-skill\lambda
npm install
npm test
npm run check
```

## Alexa development

The Alexa Skill is stored under `alexa-skill/` using the ASK CLI project layout. Its French invocation name is `hermes`. For short replies it can use the text turn endpoint; for long-running Hermes work it immediately starts a secure Alexa AudioPlayer stream through:

```http
POST ${BRIDGE_URL}/v1/channels/alexa/audio/jobs
```

Set `BRIDGE_URL` in the Lambda environment to the public HTTPS base URL of the bridge. The current Alexa-hosted workflow is synchronized through the Alexa Developer Console's Git workflow. The package also retains ASK CLI metadata for future AWS-hosted deployment.

Example utterances:

```text
Alexa, ouvre Hermes
Alexa, envoie bonjour
```

The complete deployment sequence, audio-stream security contract, cancellation behaviour, and physical-Echo test plan are in [docs/alexa-live-audio.md](docs/alexa-live-audio.md).

Expected initial spoken response:

```text
Ok patron, je lance le job.
```

See [docs/alexa-skill.md](docs/alexa-skill.md) for the detailed setup and [docs/architecture.md](docs/architecture.md) for the current integration boundary.

## Logging

The Java service uses SLF4J with Logback and emits one JSON event per line to stdout. This is intended for container log collection by Loki/Grafana. Request text, credentials, and other sensitive payloads are not logged.

## Roadmap

- add authenticated gateway configuration;
- add Alexa account linking and a device configuration dashboard;
- add Google Home and custom-device adapters;
- add deployment manifests and production observability.

## Scope and safety

Hermes Bridge is a transport and routing boundary, not a replacement for Hermes Agent. It must not receive Docker access, SSH keys, Hermes profile secrets, or administrative credentials. Production deployments must add authentication, rate limiting, request-size limits, HTTPS, and explicit authorization for agent actions.
