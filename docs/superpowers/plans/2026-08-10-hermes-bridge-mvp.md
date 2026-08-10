# Hermes Bridge MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a versioned Alexa-to-HTTP bridge whose Spring WebFlux endpoint returns `Bien reçu chef` for valid text turns.

**Architecture:** The repository is a small monorepo. `bridge/` contains a Java 21 Spring Boot WebFlux service with an explicit SLF4J-to-Logback JSON logging pipeline. `alexa-skill/` contains the ASK CLI skill package and Node.js Lambda adapter; the Lambda forwards Alexa text and metadata to the bridge and speaks the returned `text`.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring WebFlux/Reactor Netty, Gradle Kotlin DSL, SLF4J, Logback, logstash-logback-encoder, Node.js, ASK CLI v2, Alexa Custom Skill.

## Global Constraints

- The repository is `G:\Documents\DEV\hermes-bridge`, separate from Hermes Core.
- The MVP response is exactly `Bien reçu chef`.
- The backend has no database, Hermes call, OAuth, account linking, Google Home channel, or production authentication.
- The backend endpoint is `POST /v1/channels/alexa/turn`.
- The health endpoint is `GET /actuator/health`.
- Logback is the explicit Java logging implementation; JSON is emitted to stdout, with no application-managed log files.
- User text, authorization material, and sensitive payloads must never be logged.
- `dashboard/` and `landing-page/` are reserved directories with documentation only in this MVP.

---

### Task 1: Scaffold the repository and Gradle Kotlin DSL service

**Files:**
- Create: `README.md`
- Create: `.gitignore`
- Create: `bridge/settings.gradle.kts`
- Create: `bridge/build.gradle.kts`
- Create: `bridge/gradle.properties`
- Create: `bridge/src/main/java/com/hermesbridge/BridgeApplication.java`
- Create: `bridge/src/main/resources/application.yml`
- Create: `dashboard/README.md`
- Create: `landing-page/README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/alexa-skill.md`

**Interfaces:**
- Produces a runnable Spring Boot application named `hermes-bridge` on the default HTTP port 8080.
- Exposes the management health endpoint through `spring-boot-starter-actuator`.

- [ ] **Step 1: Create the application build files**

Use Spring Boot 4.1.x, Java toolchain 21, `spring-boot-starter-webflux`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, Logback JSON support, and test dependencies including `spring-boot-starter-test` and `reactor-test`.

- [ ] **Step 2: Add application metadata and reserved-area documentation**

Set `spring.application.name: hermes-bridge`, expose `health` on `/actuator`, and document that the dashboard and landing page are future scopes.

- [ ] **Step 3: Add repository ignore rules**

Ignore Gradle output, IDE files, local environment files, ASK CLI `.ask/` artifacts, Node dependencies, and build output while keeping `ask-resources.json`, the skill package, Lambda source, and documentation tracked.

- [ ] **Step 4: Run the empty application test cycle**

Run `./gradlew test` from `bridge/` and then `./gradlew bootRun` with a bounded local smoke check against `/actuator/health`.

- [ ] **Step 5: Commit the scaffold**

```text
git add README.md .gitignore bridge dashboard landing-page docs/architecture.md docs/alexa-skill.md
git commit -m "feat: scaffold Hermes Bridge service"
```

### Task 2: Implement the Alexa turn endpoint test-first

**Files:**
- Create: `bridge/src/main/java/com/hermesbridge/channels/alexa/AlexaTurnRequest.java`
- Create: `bridge/src/main/java/com/hermesbridge/channels/alexa/AlexaTurnResponse.java`
- Create: `bridge/src/main/java/com/hermesbridge/channels/alexa/AlexaTurnController.java`
- Create: `bridge/src/main/java/com/hermesbridge/web/ApiError.java`
- Create: `bridge/src/main/java/com/hermesbridge/web/GlobalExceptionHandler.java`
- Create: `bridge/src/test/java/com/hermesbridge/channels/alexa/AlexaTurnControllerTest.java`

**Interfaces:**
- `POST /v1/channels/alexa/turn` consumes `AlexaTurnRequest(text, deviceId, sessionId, requestId)`.
- `AlexaTurnRequest.text` is required and must be nonblank; metadata fields are optional strings.
- A valid request returns `AlexaTurnResponse(text = "Bien reçu chef")` with HTTP 200.
- Invalid JSON or validation errors return HTTP 400 with a stable JSON error containing `code`, `message`, and `path`.

- [ ] **Step 1: Write the failing valid-request WebFlux test**

Use `@WebFluxTest(AlexaTurnController.class)` and `WebTestClient` to POST a payload containing `text`, `deviceId`, `sessionId`, and `requestId`; assert HTTP 200 and exact JSON `{ "text": "Bien reçu chef" }`.

- [ ] **Step 2: Write the failing invalid-request test**

POST `{ "text": "   " }` and assert HTTP 400 with `code` equal to `VALIDATION_ERROR` and no stack trace in the response.

- [ ] **Step 3: Implement immutable request/response records and controller**

Use Jakarta validation annotations on the request record and return `Mono<AlexaTurnResponse>` from the controller without calling Hermes or a database.

- [ ] **Step 4: Implement the JSON error handler**

Map `WebExchangeBindException` and malformed JSON to the documented 400 response. Do not echo the submitted text in error messages.

- [ ] **Step 5: Run the focused tests**

Run `./gradlew test --tests '*AlexaTurnControllerTest'` and confirm all cases pass.

- [ ] **Step 6: Commit the endpoint**

```text
git add bridge/src/main/java bridge/src/test/java
git commit -m "feat: add Alexa turn endpoint"
```

### Task 3: Add structured Logback JSON logging

**Files:**
- Create: `bridge/src/main/resources/logback-spring.xml`
- Modify: `bridge/src/main/java/com/hermesbridge/channels/alexa/AlexaTurnController.java`
- Create: `bridge/src/test/java/com/hermesbridge/logging/StructuredLoggingTest.java`
- Modify: `docs/architecture.md`

**Interfaces:**
- Each accepted Alexa turn emits one structured event containing `service`, `channel`, and safe request correlation metadata.
- The event contains no `text`, authorization header, or raw request body.

- [ ] **Step 1: Write the logging behavior test**

Capture the application console output while exercising the endpoint; parse each emitted event as one JSON object and assert that a valid turn contains `channel: "alexa"` and the supplied `requestId`, while the submitted text is absent.

- [ ] **Step 2: Configure Logback JSON output**

Configure a console appender in `logback-spring.xml` using `logstash-logback-encoder`, with ISO timestamps, level, logger, service name, and MDC/key-value fields. Keep stdout as the only application sink.

- [ ] **Step 3: Add safe request logging**

Log only `channel`, `requestId`, `deviceId`, `sessionId`, HTTP result, and elapsed time. Use null-safe values and never log the request text.

- [ ] **Step 4: Run logging verification**

Run `./gradlew test --tests '*StructuredLoggingTest'` and manually pipe a local request through the service to verify one-line JSON output.

- [ ] **Step 5: Commit logging**

```text
git add bridge/src/main bridge/src/test docs/architecture.md
git commit -m "feat: add structured Logback logging"
```

### Task 4: Add the versioned Alexa Skill package and Lambda adapter

**Files:**
- Create: `alexa-skill/skill-package/skill.json`
- Create: `alexa-skill/skill-package/interactionModels/custom/fr-FR.json`
- Create: `alexa-skill/lambda/index.js`
- Create: `alexa-skill/lambda/package.json`
- Create: `alexa-skill/ask-resources.json`
- Create: `alexa-skill/README.md`
- Modify: `docs/alexa-skill.md`

**Interfaces:**
- The French custom model defines invocation name `hermes`.
- `SendTextIntent` captures one `AMAZON.SearchQuery` slot named `message` using carrier phrases such as `envoie {message}` and `dis {message}`.
- Lambda POSTs `{ text, deviceId, sessionId, requestId }` to the configured bridge URL.
- Lambda speaks the bridge response’s `text` property; if the bridge fails, it speaks a short deterministic error message.

- [ ] **Step 1: Add the ASK CLI v2 skill package**

Use the `skill-package/` plus `lambda/` layout, include the required built-in intents, and keep deployment metadata in `ask-resources.json`. Do not commit generated `.ask/` files or credentials.

- [ ] **Step 2: Add the Lambda implementation**

Use the Alexa Skills Kit SDK for Node.js, validate the bridge response shape, read the bridge URL from configuration, and send the Alexa `deviceId`, `sessionId`, and request identifier without logging secrets or user text.

- [ ] **Step 3: Add Lambda tests and local fixtures**

Test the intent handler with a mocked `fetch` response returning `{ text: "Bien reçu chef" }` and a mocked failure returning the deterministic error speech. Add a sample Alexa request JSON under `alexa-skill/test/fixtures/`.

- [ ] **Step 4: Document ASK CLI deployment and console testing**

Document `ask configure`, the development deployment command, the bridge URL configuration, the Alexa simulator utterance, and the expected spoken response.

- [ ] **Step 5: Run skill validation**

Run the Lambda test command from `alexa-skill/`, validate the JSON interaction model with ASK CLI tooling, and run the local bridge integration fixture.

- [ ] **Step 6: Commit the skill**

```text
git add alexa-skill docs/alexa-skill.md
git commit -m "feat: add versioned Alexa skill adapter"
```

### Task 5: Complete documentation and end-to-end verification

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/alexa-skill.md`
- Create: `scripts/smoke-test.ps1`

**Interfaces:**
- A new developer can start the bridge, submit a sample Alexa payload, inspect JSON logs, run tests, and understand the Alexa deployment path from the repository documentation.

- [ ] **Step 1: Document the end-to-end flow**

Describe the Alexa Lambda → bridge endpoint contract, exact fixed response, development-only security boundary, and future Hermes routing boundary.

- [ ] **Step 2: Add a Windows-compatible smoke script**

Create a PowerShell script that posts a fixed sample payload to a configurable base URL and asserts the response status and exact `text` value.

- [ ] **Step 3: Run all verification commands**

Run `./gradlew test`, the Lambda tests, ASK CLI model validation, and the PowerShell smoke test against a running local bridge.

- [ ] **Step 4: Review the final tree and working state**

Check that no secrets, `.ask/` artifacts, `node_modules`, build output, or generated credentials are tracked. Confirm `git diff --check` is clean.

- [ ] **Step 5: Commit documentation and verification**

```text
git add README.md docs scripts/smoke-test.ps1
git commit -m "docs: document Hermes Bridge MVP"
```

