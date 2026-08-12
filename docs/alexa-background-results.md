# Alexa background results

Configure `DISCORD_WEBHOOK_URL` as a secret environment variable of Hermes Bridge.
It is used only to post the completed background result to Discord and must never
be committed, exposed to Alexa, or written to logs.

Say `en arrière-plan` in an Alexa request to opt in immediately. Alexa confirms
the launch and does not open the audio player; Hermes then completes normally and
the Bridge posts its final text to Discord.

Hermes may also opt in before producing normal output with:

```text
[[background]]
```

For foreground work, Hermes can make concise spoken progress announcements:

```text
[[progress:Je consulte les données]]
```

Both control markers are removed from the final answer. The background marker
should be emitted before regular answer text. While a foreground response is
being prepared, the Bridge emits a low-volume original waiting loop; it stops
when the first Kokoro speech MP3 is available.
