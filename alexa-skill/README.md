# Hermes Alexa Skill

This directory contains the versioned Alexa Custom Skill and its Node.js Lambda adapter.

## Local checks

```powershell
cd lambda
npm install
npm test
npm run check
```

## Bridge URL

Configure the Lambda environment variable `BRIDGE_URL` with the public base URL of the running bridge. The Lambda calls:

```text
POST ${BRIDGE_URL}/v1/channels/alexa/turn
```

It sends the recognized text and Alexa request metadata, then speaks the returned `text` field.

## ASK CLI and Alexa-hosted deployment

The repository uses the ASK CLI v2 package layout so that the manifest, interaction model, and Lambda source remain versioned. For the Alexa-hosted skill created in the Developer Console, synchronize this directory through the skill's Git workflow and test it in the development stage.

If the skill is later moved to an AWS-hosted Lambda deployment, install and configure ASK CLI v2 and run from this directory:

```powershell
ask deploy
```

The interaction model is French with invocation name `hermes`. Test with:

```text
Alexa, ouvre Hermes
Alexa, envoie bonjour
```

The expected response is `Bien reçu chef`.
