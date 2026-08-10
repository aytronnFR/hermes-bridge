package com.hermesbridge.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredLoggingTest {

    @Test
    void configuresLogbackForJsonConsoleOutputWithMdc() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/logback-spring.xml")) {
            String configuration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(configuration.contains("LoggingEventCompositeJsonEncoder"));
            assertTrue(configuration.contains("<mdc/>"));
            assertTrue(configuration.contains("JSON_CONSOLE"));
        }
    }
}

