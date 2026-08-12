from __future__ import annotations

import io
import os
import subprocess
from functools import lru_cache

import numpy as np
import soundfile as sf
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

app = FastAPI(title="Hermes Kokoro TTS")


class SpeechRequest(BaseModel):
    input: str = Field(min_length=1, max_length=4000)
    model: str = "kokoro"
    voice: str = "ff_siwis"
    response_format: str = "mp3"


@lru_cache(maxsize=1)
def pipeline():
    from kokoro import KPipeline
    return KPipeline(lang_code="f")


def synthesize_mp3(text: str, voice: str) -> bytes:
    chunks = [audio for _, _, audio in pipeline()(text, voice=voice) if len(audio)]
    if not chunks:
        raise ValueError("Kokoro produced no audio")
    wav = io.BytesIO()
    sf.write(wav, np.concatenate(chunks), 24000, format="WAV")
    process = subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-i", "pipe:0", "-f", "mp3", "-b:a", "64k", "pipe:1"],
        input=wav.getvalue(), capture_output=True, check=False,
    )
    if process.returncode or not process.stdout:
        raise RuntimeError("MP3 encoding failed")
    return process.stdout


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/v1/audio/speech")
def speech(request: SpeechRequest):
    if request.response_format != "mp3":
        raise HTTPException(400, "Only MP3 output is supported")
    try:
        return Response(synthesize_mp3(request.input, request.voice), media_type="audio/mpeg")
    except Exception as error:
        raise HTTPException(502, "Speech synthesis failed") from error
