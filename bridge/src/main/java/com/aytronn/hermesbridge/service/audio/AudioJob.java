package com.aytronn.hermesbridge.service.audio;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

public final class AudioJob {
  private final String id;
  private final String token;
  private final String ownerUserId;
  private final String ownerDeviceId;
  private final String text;
  private final String preparedResponse;
  private final Instant expiresAt;
  private final Sinks.Many<byte[]> audio = Sinks.many().replay().all();
  private final Sinks.One<Void> completed = Sinks.one();
  private final Sinks.One<Void> speechStarted = Sinks.one();
  private final StringBuilder finalResult = new StringBuilder();
  private final StringBuilder pendingSpeech = new StringBuilder();
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean backgroundRequested;
  private volatile Disposable upstream;

  public AudioJob(String id, String token, String ownerUserId, String ownerDeviceId, String text, Instant expiresAt,
      boolean backgroundRequested, String preparedResponse) {
    this.id = id;
    this.token = token;
    this.ownerUserId = ownerUserId;
    this.ownerDeviceId = ownerDeviceId;
    this.text = text;
    this.preparedResponse = preparedResponse;
    this.expiresAt = expiresAt;
    this.backgroundRequested = new AtomicBoolean(backgroundRequested);
  }
  public String id() { return id; }
  public String token() { return token; }
  public String ownerUserId() { return ownerUserId; }
  public String ownerDeviceId() { return ownerDeviceId; }
  public String text() { return text; }
  public String preparedResponse() { return preparedResponse; }
  public Instant expiresAt() { return expiresAt; }
  public Sinks.Many<byte[]> audio() { return audio; }
  public Sinks.One<Void> completed() { return completed; }
  public Sinks.One<Void> speechStarted() { return speechStarted; }
  public synchronized void appendFinalResult(String text) { finalResult.append(text); }
  public synchronized String finalResult() { return finalResult.toString().trim(); }
  public synchronized java.util.List<String> appendSpeechFragment(String text) {
    pendingSpeech.append(text);
    return SentenceChunker.drainCompleteSentences(pendingSpeech);
  }
  public synchronized String flushPendingSpeech() {
    String tail = pendingSpeech.toString().trim();
    pendingSpeech.setLength(0);
    return tail;
  }
  public boolean markStarted() { return started.compareAndSet(false, true); }
  public boolean backgroundRequested() { return backgroundRequested.get(); }
  public void requestBackground() { backgroundRequested.set(true); }
  public void upstream(Disposable upstream) { this.upstream = upstream; }
  public void cancel() { if (upstream != null) upstream.dispose(); audio.tryEmitComplete(); completed.tryEmitEmpty(); }
}
