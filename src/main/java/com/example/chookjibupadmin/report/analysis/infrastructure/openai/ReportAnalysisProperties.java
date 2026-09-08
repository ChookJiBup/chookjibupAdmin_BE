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
        long pollDelayMillis,
        Duration jobTimeout,
        long timeoutScanDelayMillis
) {

    private static final String DEFAULT_PROVIDER = "disabled";
    private static final String DEFAULT_MODEL = "gpt-5.6";
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    // 관리자 화면이 배치도 분석에 쓰는 대기 한도(7분)와 같은 기준을 사용한다.
    private static final Duration DEFAULT_JOB_TIMEOUT = Duration.ofMinutes(7);

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

    /**
     * 처리 중인 작업을 회수하기까지 기다리는 제한 시간을 반환한다.
     */
    public Duration jobTimeoutOrDefault() {
        return jobTimeout == null
                || jobTimeout.isZero()
                || jobTimeout.isNegative()
                ? DEFAULT_JOB_TIMEOUT
                : jobTimeout;
    }

    public boolean isOpenAiEnabled() {
        return "openai".equalsIgnoreCase(providerOrDefault());
    }
}
