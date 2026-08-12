FROM python:3.12-slim

RUN apt-get update \
    && apt-get install --no-install-recommends -y ffmpeg libsndfile1 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 appuser

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app ./app

USER appuser
EXPOSE 8880
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8880"]
