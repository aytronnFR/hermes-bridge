package com.aytronn.hermesbridge.service.audio;

import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Flux;

/** Parses Hermes control markers without exposing them to the user-facing response. */
final class HermesStreamDirectiveParser {
  private static final String START = "[[";
  private static final String END = "]]";
  private static final String PROGRESS_PREFIX = "[[progress:";
  private static final String BACKGROUND = "[[background]]";

  Flux<HermesStreamEvent> parse(Flux<String> deltas) {
    StringBuilder buffer = new StringBuilder();
    return deltas.concatMap(delta -> Flux.fromIterable(accept(buffer, delta)))
        .concatWith(Flux.defer(() -> Flux.fromIterable(finish(buffer))));
  }

  private List<HermesStreamEvent> accept(StringBuilder buffer, String delta) {
    buffer.append(delta);
    List<HermesStreamEvent> events = new ArrayList<>();
    while (!buffer.isEmpty()) {
      int marker = buffer.indexOf(START);
      if (marker < 0) {
        int keep = buffer.charAt(buffer.length() - 1) == '[' ? 1 : 0;
        emitText(events, buffer.substring(0, buffer.length() - keep));
        buffer.delete(0, buffer.length() - keep);
        return events;
      }
      if (marker > 0) {
        emitText(events, buffer.substring(0, marker));
        buffer.delete(0, marker);
      }
      int end = buffer.indexOf(END);
      if (end < 0) return events;
      String markerValue = buffer.substring(0, end + END.length());
      buffer.delete(0, end + END.length());
      if (BACKGROUND.equals(markerValue)) {
        events.add(HermesStreamEvent.Background.INSTANCE);
      } else if (markerValue.startsWith(PROGRESS_PREFIX) && markerValue.length() > PROGRESS_PREFIX.length() + END.length()) {
        String message = markerValue.substring(PROGRESS_PREFIX.length(), markerValue.length() - END.length()).trim();
        if (!message.isEmpty()) events.add(new HermesStreamEvent.Progress(message));
      } else {
        emitText(events, markerValue);
      }
    }
    return events;
  }

  private List<HermesStreamEvent> finish(StringBuilder buffer) {
    List<HermesStreamEvent> events = new ArrayList<>();
    emitText(events, buffer.toString());
    buffer.setLength(0);
    return events;
  }

  private static void emitText(List<HermesStreamEvent> events, String text) {
    if (text.isEmpty()) return;
    if (!events.isEmpty() && events.getLast() instanceof HermesStreamEvent.Text previous) {
      events.set(events.size() - 1, new HermesStreamEvent.Text(previous.value() + text));
      return;
    }
    events.add(new HermesStreamEvent.Text(text));
  }
}
