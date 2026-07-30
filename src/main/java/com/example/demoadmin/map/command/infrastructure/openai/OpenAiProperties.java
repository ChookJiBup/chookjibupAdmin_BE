package com.example.demoadmin.map.command.infrastructure.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model
) {

    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }

        return baseUrl;
    }

    public String resolvedModel() {
        if (model == null || model.isBlank()) {
            return "gpt-5.6";
        }

        return model;
    }
}
