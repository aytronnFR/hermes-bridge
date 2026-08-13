"""Lightweight voice finishing combined with the existing MP3 encode."""


def ffmpeg_command() -> list[str]:
    return [
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-i", "pipe:0",
        "-af",
        "highpass=f=80,"
        "equalizer=f=2800:t=q:w=1.2:g=2,"
        "acompressor=threshold=-18dB:ratio=1.5:attack=20:release=180:makeup=1.5dB",
        "-f", "mp3", "-b:a", "64k", "pipe:1",
    ]
