package com.aytronn.hermesbridge.service.audio;

public class AudioJobNotFoundException extends RuntimeException {

  public AudioJobNotFoundException() {
    super("Audio job not found");
  }
}
