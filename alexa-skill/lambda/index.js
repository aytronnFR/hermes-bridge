const Alexa = require('ask-sdk-core');

const BRIDGE_URL = process.env.BRIDGE_URL;
const BRIDGE_API_KEY = process.env.BRIDGE_API_KEY;
const BRIDGE_TIMEOUT_MS = 7_000;
const activePlaybackTokens = new Map();

const LaunchRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'LaunchRequest';
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder
      .speak('Oui ?')
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
      const bridgeResponse = await createAudioJob(handlerInput, text);
      activePlaybackTokens.set(alexaUserId(handlerInput), bridgeResponse.playbackToken);
      return handlerInput.responseBuilder
        .speak('Ok patron, je lance le job.')
        .addAudioPlayerPlayDirective(
          'REPLACE_ALL', bridgeResponse.streamUrl, bridgeResponse.playbackToken, 0, null,
          { title: 'Hermes', subtitle: 'Réponse en cours' }
        )
        .withShouldEndSession(true)
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
      && ['AMAZON.CancelIntent', 'AMAZON.StopIntent', 'AMAZON.PauseIntent'].includes(
        Alexa.getIntentName(handlerInput.requestEnvelope)
      );
  },
  async handle(handlerInput) {
    await cancelAudioJob(handlerInput);
    return handlerInput.responseBuilder.addAudioPlayerStopDirective().getResponse();
  }
};

const AudioPlaybackStoppedHandler = {
  canHandle(handlerInput) {
    return ['AudioPlayer.PlaybackStopped', 'AudioPlayer.PlaybackFailed'].includes(
      Alexa.getRequestType(handlerInput.requestEnvelope));
  },
  async handle(handlerInput) {
    await cancelAudioJob(handlerInput);
    return handlerInput.responseBuilder.getResponse();
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

async function createAudioJob(handlerInput, text) {
  if (!BRIDGE_URL) {
    throw new Error('BRIDGE_URL is not configured');
  }
  if (!BRIDGE_API_KEY) {
    throw new Error('BRIDGE_API_KEY is not configured');
  }

  const envelope = handlerInput.requestEnvelope;
  const system = envelope.context?.System || {};
  const request = envelope.request || {};
  const deviceId = system.device?.deviceId;
  const userId = system.user?.userId;
  if (!deviceId || !userId) {
    throw new Error('Alexa user and device identifiers are required');
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), BRIDGE_TIMEOUT_MS);

  try {
    const response = await fetch(`${BRIDGE_URL.replace(/\/$/, '')}/v1/channels/alexa/audio/jobs`, {
      method: 'POST',
      headers: {
        authorization: `Bearer ${BRIDGE_API_KEY}`,
        'content-type': 'application/json'
      },
      body: JSON.stringify({
        text,
        userId,
        deviceId
      }),
      signal: controller.signal
    });

    if (!response.ok) {
      throw new Error(`Bridge returned HTTP ${response.status}`);
    }

    const body = await response.json();
    if (!body || typeof body.streamUrl !== 'string' || typeof body.playbackToken !== 'string') {
      throw new Error('Bridge returned an invalid response');
    }
    return body;
  } finally {
    clearTimeout(timeout);
  }
}

async function cancelAudioJob(handlerInput) {
  if (!BRIDGE_URL || !BRIDGE_API_KEY) return;
  const envelope = handlerInput.requestEnvelope;
  const userId = alexaUserId(handlerInput);
  const token = envelope.request?.token || activePlaybackTokens.get(userId);
  if (!userId || !token) return;
  try {
    await fetch(`${BRIDGE_URL.replace(/\/$/, '')}/v1/channels/alexa/audio/cancel`, {
      method: 'POST',
      headers: { authorization: `Bearer ${BRIDGE_API_KEY}`, 'content-type': 'application/json' },
      body: JSON.stringify({ playbackToken: token, userId })
    });
  } catch (error) {
    console.error(JSON.stringify({ event: 'audio_cancel_failed', error: error.message }));
  } finally {
    activePlaybackTokens.delete(userId);
  }
}

function alexaUserId(handlerInput) {
  return handlerInput.requestEnvelope.context?.System?.user?.userId;
}

const skill = Alexa.SkillBuilders.custom()
  .addRequestHandlers(
    LaunchRequestHandler,
    SendTextIntentHandler,
    HelpIntentHandler,
    ExitIntentHandler,
    FallbackIntentHandler,
    AudioPlaybackStoppedHandler,
    SessionEndedRequestHandler
  )
  .addErrorHandlers(ErrorHandler)
  .create();

// Node.js 24 no longer supports callback-based Lambda handlers.
exports.handler = async (event, context) => skill.invoke(event, context);

exports.createAudioJob = createAudioJob;

