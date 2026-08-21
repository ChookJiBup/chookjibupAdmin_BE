package com.example.chookjibupadmin.report.analysis.infrastructure.openai;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.report.analysis")
public record ReportAnalysisProperties(
        String provider,
        URI baseUrl,
        String apiKey,
        String model,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        long pollDelayMillis
) {

    private static final String DEFAULT_PROVIDER = "disabled";
    private static final String DEFAULT_MODEL = "gpt-5.6";
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    public String providerOrDefault() {
        return provider == null || provider.isBlank()
                ? DEFAULT_PROVIDER
                : provider;
    }

    public String modelOrDefault() {
        return model == null || model.isBlank() ? DEFAULT_MODEL : model;
    }

    public int maxAttemptsOrDefault() {
        return maxAttempts <= 0 ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
    }

    public boolean isOpenAiEnabled() {
        return "openai".equalsIgnoreCase(providerOrDefault());
    }
}
