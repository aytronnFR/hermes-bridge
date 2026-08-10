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

## ASK CLI

Install and configure ASK CLI v2, then from this directory deploy the development resources:

```powershell
ask deploy
```

The interaction model is French with invocation name `hermes`. Test with:

```text
Alexa, ouvre Hermes
Alexa, envoie bonjour
```

The expected response is `Bien reçu chef`.

