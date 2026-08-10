# Alexa Skill

The Alexa Skill is versioned under `alexa-skill/` using the ASK CLI project layout. It currently forwards a text turn to the bridge and speaks the bridge response.

The development interaction is French and uses the invocation name `hermes`. The current skill uses Alexa-hosted Node.js, so the Developer Console Git workflow is the deployment path. ASK CLI remains useful for managing and validating the versioned package; `ask deploy` is reserved for a later AWS-hosted deployment choice.

The current Lambda requires the `BRIDGE_URL` environment variable. It sends the recognized text and device/session/request metadata to the bridge. It does not yet use Alexa account linking or persist a Hermes conversation.
# Hermes Gateway connection

The Lambda keeps the public bridge URL in `BRIDGE_URL`. The bridge authenticates to the official Hermes Agent API with these backend-only variables:

```text
HERMES_GATEWAY_URL=https://hermes-gateway-api.aytronn.com
HERMES_GATEWAY_API_KEY=<Hermes API key>
HERMES_GATEWAY_MODEL=alexa
```

Alexa's `deviceId` becomes the Hermes named conversation `alexa:<deviceId>`. Hermes retains that conversation server-side, including after bridge restarts. The key is configured as `API_SERVER_KEY` in the Hermes profile and is passed by the bridge as a Bearer token. See the root README for secure generation and tunnel setup.

The Lambda also has its own `BRIDGE_API_KEY` environment variable. It must have
the same value as `HERMES_BRIDGE_API_KEY` in the bridge deployment. This is a
separate secret from the bridge-to-Hermes `HERMES_GATEWAY_API_KEY` and protects
the public Alexa turn endpoint.
