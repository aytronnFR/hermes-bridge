const { test, afterEach } = require('node:test');
const assert = require('node:assert/strict');

process.env.BRIDGE_URL = 'https://bridge.example.test';
const skill = require('./index.js');

const originalFetch = global.fetch;

afterEach(() => {
  global.fetch = originalFetch;
});

function envelope() {
  return {
    version: '1.0',
    session: { sessionId: 'session-1' },
    context: {
      System: {
        device: { deviceId: 'device-1' }
      }
    },
    request: {
      type: 'IntentRequest',
      requestId: 'request-1',
      intent: {
        name: 'SendTextIntent',
        slots: { message: { name: 'message', value: 'bonjour' } }
      }
    }
  };
}

function invoke(event) {
  return new Promise((resolve, reject) => {
    skill.handler(event, {}, (error, response) => {
      if (error) {
        reject(error);
      } else {
        resolve(response);
      }
    });
  });
}

test('forwards Alexa text and returns the bridge response', async () => {
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return {
      ok: true,
      async json() {
        return { text: 'Bien reçu chef' };
      }
    };
  };

  const result = await invoke(envelope());

  assert.equal(request.url, 'https://bridge.example.test/v1/channels/alexa/turn');
  assert.deepEqual(JSON.parse(request.options.body), {
    text: 'bonjour',
    deviceId: 'device-1',
    sessionId: 'session-1',
    requestId: 'request-1'
  });
  assert.equal(result.response.outputSpeech.ssml, '<speak>Bien reçu chef</speak>');
});

test('returns a spoken failure when the bridge is unavailable', async () => {
  global.fetch = async () => {
    throw new Error('network unavailable');
  };

  const result = await invoke(envelope());

  assert.equal(result.response.outputSpeech.ssml, '<speak>Je n’ai pas réussi à joindre Hermes.</speak>');
});
