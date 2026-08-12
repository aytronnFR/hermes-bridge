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
  private final Instant expiresAt;
  private final Sinks.Many<byte[]> audio = Sinks.many().replay().all();
  private final Sinks.One<Void> completed = Sinks.one();
  private final AtomicBoolean started = new AtomicBoolean();
  private volatile Disposable upstream;

  public AudioJob(String id, String token, String ownerUserId, String ownerDeviceId, String text, Instant expiresAt) {
    this.id = id;
    this.token = token;
    this.ownerUserId = ownerUserId;
    this.ownerDeviceId = ownerDeviceId;
    this.text = text;
    this.expiresAt = expiresAt;
  }
  public String id() { return id; }
  public String token() { return token; }
  public String ownerUserId() { return ownerUserId; }
  public String ownerDeviceId() { return ownerDeviceId; }
  public String text() { return text; }
  public Instant expiresAt() { return expiresAt; }
  public Sinks.Many<byte[]> audio() { return audio; }
  public Sinks.One<Void> completed() { return completed; }
  public boolean markStarted() { return started.compareAndSet(false, true); }
  public void upstream(Disposable upstream) { this.upstream = upstream; }
  public void cancel() { if (upstream != null) upstream.dispose(); audio.tryEmitComplete(); completed.tryEmitEmpty(); }
}
