package com.aytronn.hermesbridge.service.audio;

sealed interface HermesStreamEvent permits HermesStreamEvent.Text, HermesStreamEvent.Progress,
    HermesStreamEvent.Background {

  record Text(String value) implements HermesStreamEvent { }

  record Progress(String value) implements HermesStreamEvent { }

  enum Background implements HermesStreamEvent {
    INSTANCE
  }
}
