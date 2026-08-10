# Hermes Bridge

Public channel bridge for Hermes Agent. The first milestone connects an Alexa Custom Skill to a small Spring WebFlux API.

## MVP status

The current MVP accepts an Alexa turn at `POST /v1/channels/alexa/turn` and returns:

```json
{"text":"Bien reçu chef"}
```

It does not yet call Hermes, persist conversations, authenticate users, or implement the dashboard and landing page.

## Repository layout

- `bridge/` — Java 21 / Spring Boot WebFlux service.
- `alexa-skill/` — versioned Alexa Skill package and Lambda adapter.
- `dashboard/` — reserved for the future configuration UI.
- `landing-page/` — reserved for the future public site.
- `docs/` — architecture, skill setup, plans, and design specifications.

## Run the bridge

From `bridge/`:

```powershell
.\gradlew.bat bootRun
```

The health check is available at `http://localhost:8080/actuator/health`.

## Test the bridge

```powershell
.\gradlew.bat test
```

See [docs/alexa-skill.md](docs/alexa-skill.md) for the Alexa setup and [docs/architecture.md](docs/architecture.md) for the current boundary.

## Smoke test

With the bridge running, execute from the repository root:

```powershell
.\scripts\smoke-test.ps1
```

