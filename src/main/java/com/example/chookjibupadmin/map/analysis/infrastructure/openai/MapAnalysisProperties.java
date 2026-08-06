package com.example.chookjibupadmin.map.analysis.infrastructure.openai;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.map.analysis")
public record MapAnalysisProperties(
        String provider,
        URI baseUrl,
        String apiKey,
        String model,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        long pollDelayMillis,
        long maxInputBytes
) {

    private static final String DEFAULT_PROVIDER = "disabled";
    private static final String DEFAULT_MODEL = "gpt-5.6";
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_MAX_INPUT_BYTES = 25L * 1024 * 1024;

    public String providerOrDefault() {
        return provider == null ? DEFAULT_PROVIDER : provider;
    }

    public String modelOrDefault() {
        return model == null || model.isBlank()
                ? DEFAULT_MODEL
                : model;
    }

    public int maxAttemptsOrDefault() {
        return maxAttempts <= 0
                ? DEFAULT_MAX_ATTEMPTS
                : maxAttempts;
    }

    public long maxInputBytesOrDefault() {
        return maxInputBytes <= 0
                ? DEFAULT_MAX_INPUT_BYTES
                : maxInputBytes;
    }
}
