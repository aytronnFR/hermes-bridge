# -------- Stage 1: build --------
FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /src

COPY . .

RUN chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

# -------- Stage 2: runtime --------
FROM eclipse-temurin:25-jre-jammy

LABEL org.opencontainers.image.source="https://github.com/aytronnFR/hermes-bridge"

RUN apt-get update \
    && apt-get install --no-install-recommends -y libgomp1 libstdc++6 wget \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 appuser

WORKDIR /app

COPY --from=build --chown=appuser:appuser /src/build/libs/*.jar /app/app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=90.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
