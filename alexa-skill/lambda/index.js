const Alexa = require('ask-sdk-core');

const BRIDGE_URL = process.env.BRIDGE_URL;
const BRIDGE_TIMEOUT_MS = 7000;

const LaunchRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'LaunchRequest';
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder
      .speak('Bonjour, que voulez-vous transmettre à Hermes ?')
      .reprompt('Dites-moi votre message.')
      .getResponse();
  }
};

const SendTextIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'SendTextIntent';
  },
  async handle(handlerInput) {
    const text = Alexa.getSlotValue(handlerInput.requestEnvelope, 'message');

    try {
      const bridgeResponse = await sendToBridge(handlerInput, text);
      return handlerInput.responseBuilder
        .speak(bridgeResponse.text)
        .getResponse();
    } catch (error) {
      console.error(JSON.stringify({ event: 'bridge_request_failed', error: error.message }));
      return handlerInput.responseBuilder
        .speak('Je n’ai pas réussi à joindre Hermes.')
        .getResponse();
    }
  }
};

const HelpIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.HelpIntent';
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder
      .speak('Dites, envoie bonjour, pour transmettre un message à Hermes.')
      .reprompt('Quel message voulez-vous transmettre ?')
      .getResponse();
  }
};

const ExitIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && ['AMAZON.CancelIntent', 'AMAZON.StopIntent'].includes(
        Alexa.getIntentName(handlerInput.requestEnvelope)
      );
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder
      .speak('Au revoir.')
      .getResponse();
  }
};

const FallbackIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.FallbackIntent';
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder
      .speak('Je n’ai pas compris. Dites, envoie bonjour.')
      .reprompt('Quel message voulez-vous transmettre ?')
      .getResponse();
  }
};

const SessionEndedRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'SessionEndedRequest';
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder.getResponse();
  }
};

const ErrorHandler = {
  canHandle() {
    return true;
  },
  handle(handlerInput, error) {
    console.error(JSON.stringify({ event: 'alexa_handler_failed', error: error.message }));
    return handlerInput.responseBuilder
      .speak('Une erreur est survenue. Veuillez réessayer.')
      .getResponse();
  }
};

async function sendToBridge(handlerInput, text) {
  if (!BRIDGE_URL) {
    throw new Error('BRIDGE_URL is not configured');
  }

  const envelope = handlerInput.requestEnvelope;
  const system = envelope.context?.System || {};
  const request = envelope.request || {};
  const deviceId = system.device?.deviceId;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), BRIDGE_TIMEOUT_MS);

  try {
    const response = await fetch(`${BRIDGE_URL.replace(/\/$/, '')}/v1/channels/alexa/turn`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        text,
        deviceId,
        sessionId: envelope.session?.sessionId,
        requestId: request.requestId
      }),
      signal: controller.signal
    });

    if (!response.ok) {
      throw new Error(`Bridge returned HTTP ${response.status}`);
    }

    const body = await response.json();
    if (!body || typeof body.text !== 'string' || body.text.length === 0) {
      throw new Error('Bridge returned an invalid response');
    }
    return body;
  } finally {
    clearTimeout(timeout);
  }
}

const skill = Alexa.SkillBuilders.custom()
  .addRequestHandlers(
    LaunchRequestHandler,
    SendTextIntentHandler,
    HelpIntentHandler,
    ExitIntentHandler,
    FallbackIntentHandler,
    SessionEndedRequestHandler
  )
  .addErrorHandlers(ErrorHandler)
  .create();

// Node.js 24 no longer supports callback-based Lambda handlers.
exports.handler = async (event, context) => skill.invoke(event, context);

exports.sendToBridge = sendToBridge;

