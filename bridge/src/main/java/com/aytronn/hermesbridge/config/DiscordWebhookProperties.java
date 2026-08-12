package com.aytronn.hermesbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord.webhook")
public record DiscordWebhookProperties(String url) { }
