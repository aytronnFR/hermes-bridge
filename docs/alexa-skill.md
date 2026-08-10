# Alexa Skill

The Alexa Skill is versioned under `alexa-skill/` using the ASK CLI project layout. It currently forwards a text turn to the bridge and speaks the bridge response.

The development interaction is French and uses the invocation name `hermes`. The current skill uses Alexa-hosted Node.js, so the Developer Console Git workflow is the deployment path. ASK CLI remains useful for managing and validating the versioned package; `ask deploy` is reserved for a later AWS-hosted deployment choice.

The current Lambda requires the `BRIDGE_URL` environment variable. It sends the recognized text and device/session/request metadata to the bridge. It does not yet use Alexa account linking or persist a Hermes conversation.
