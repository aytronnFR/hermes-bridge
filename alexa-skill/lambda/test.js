const { test, afterEach } = require('node:test');
const assert = require('node:assert/strict');

process.env.BRIDGE_URL = 'https://bridge.example.test';
process.env.BRIDGE_API_KEY = 'lambda-test-key';
const skill = require('./index.js');
const originalFetch = global.fetch;

afterEach(() => { global.fetch = originalFetch; });

function envelope(request = {}) {
  return {
    version: '1.0',
    session: { sessionId: 'session-1' },
    context: { System: { device: { deviceId: 'device-1' }, user: { userId: 'user-1' } } },
    request: {
      type: 'IntentRequest', requestId: 'request-1',
      intent: { name: 'SendTextIntent', slots: { message: { name: 'message', value: 'bonjour' } } },
      ...request
    }
  };
}

test('starts a secure audio job and immediately returns an AudioPlayer directive', async () => {
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return { ok: true, async json() { return {
      jobId: 'job-1',
      streamUrl: 'https://bridge.example.test/v1/channels/alexa/audio/streams/job-1?token=opaque',
      playbackToken: 'capability'
    }; } };
  };

  const result = await skill.handler(envelope(), {});

  assert.equal(request.url, 'https://bridge.example.test/v1/channels/alexa/audio/jobs');
  assert.equal(request.options.headers.authorization, 'Bearer lambda-test-key');
  assert.deepEqual(JSON.parse(request.options.body), {
    text: 'bonjour', userId: 'user-1', deviceId: 'device-1'
  });
  assert.equal(result.response.outputSpeech.ssml, '<speak>Ok.</speak>');
  assert.equal(result.response.directives[0].type, 'AudioPlayer.Play');
  assert.equal(result.response.directives[0].audioItem.stream.url,
    'https://bridge.example.test/v1/channels/alexa/audio/streams/job-1?token=opaque');
  assert.equal(result.response.shouldEndSession, true);
});

test('acknowledges an explicit background job without starting AudioPlayer', async () => {
  global.fetch = async () => ({ ok: true, async json() { return {
    jobId: 'job-2', streamUrl: 'https://bridge.example.test/stream', playbackToken: 'capability-2', background: true
  }; } });

  const result = await skill.handler(envelope({
    intent: { name: 'SendTextIntent', slots: { message: { name: 'message', value: 'fais le rapport en arrière-plan' } } }
  }), {});

  assert.equal(result.response.outputSpeech.ssml, '<speak>Ok.</speak>');
  assert.equal(result.response.directives, undefined);
  assert.equal(result.response.shouldEndSession, true);
});

test('reads the latest background result as an AudioPlayer stream', async () => {
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return { ok: true, async json() { return {
      jobId: 'latest-1', streamUrl: 'https://bridge.example.test/stream/latest', playbackToken: 'latest-capability'
    }; } };
  };

  const result = await skill.handler(envelope({
    intent: { name: 'SendTextIntent', slots: { message: { name: 'message', value: 'dernier résultat' } } }
  }), {});

  assert.equal(request.url, 'https://bridge.example.test/v1/channels/alexa/audio/latest');
  assert.deepEqual(JSON.parse(request.options.body), { userId: 'user-1', deviceId: 'device-1' });
  assert.equal(result.response.directives[0].type, 'AudioPlayer.Play');
});

test('reads the latest result through the dedicated Alexa command', async () => {
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return { ok: true, async json() { return {
      jobId: 'latest-command-1', streamUrl: 'https://bridge.example.test/stream/latest', playbackToken: 'latest-command-capability'
    }; } };
  };

  const result = await skill.handler(envelope({
    intent: { name: 'LatestMessageIntent', slots: {} }
  }), {});

  assert.equal(request.url, 'https://bridge.example.test/v1/channels/alexa/audio/latest');
  assert.equal(result.response.directives[0].type, 'AudioPlayer.Play');
});

test('starts a new Hermes conversation for the current Alexa device', async () => {
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return { ok: true, async json() { return {}; } };
  };

  const result = await skill.handler(envelope({
    intent: { name: 'NewConversationIntent', slots: {} }
  }), {});

  assert.equal(request.url, 'https://bridge.example.test/v1/channels/alexa/conversations/reset');
  assert.deepEqual(JSON.parse(request.options.body), { userId: 'user-1', deviceId: 'device-1' });
  assert.equal(result.response.outputSpeech.ssml,
    '<speak>C’est bon patron, on repart sur une nouvelle conversation.</speak>');
  assert.equal(result.response.shouldEndSession, true);
});

test('accepts an empty reset response from the Bridge', async () => {
  global.fetch = async () => ({
    ok: true,
    headers: { get() { return null; } },
    async json() { throw new Error('empty response must not be parsed'); }
  });

  const result = await skill.handler(envelope({
    intent: { name: 'NewConversationIntent', slots: {} }
  }), {});

  assert.equal(result.response.outputSpeech.ssml,
    '<speak>C’est bon patron, on repart sur une nouvelle conversation.</speak>');
});

test('forwards stop to Bridge using the opaque playback token', async () => {
  let request;
  global.fetch = async (url, options) => {
    request = { url, options };
    return { ok: true, async json() { return {}; } };
  };
  const result = await skill.handler(envelope({
    requestId: 'stop-1',
    token: 'capability',
    intent: { name: 'AMAZON.StopIntent', slots: {} },
  }), {});

  assert.equal(request.url, 'https://bridge.example.test/v1/channels/alexa/audio/cancel');
  assert.deepEqual(JSON.parse(request.options.body), { playbackToken: 'capability', userId: 'user-1' });
  assert.equal(result.response.directives[0].type, 'AudioPlayer.Stop');
});

test('acknowledges AudioPlayer playback lifecycle events with an empty response', async () => {
  const result = await skill.handler(envelope({
    type: 'AudioPlayer.PlaybackStarted',
    requestId: 'playback-started-1',
    token: 'capability',
    offsetInMilliseconds: 0
  }), {});

  assert.deepEqual(result.response, {});
});
