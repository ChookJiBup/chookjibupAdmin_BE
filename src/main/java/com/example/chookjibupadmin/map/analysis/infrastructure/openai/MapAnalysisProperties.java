package com.example.chookjibupadmin.map.analysis.infrastructure.openai;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.map.analysis")
public record MapAnalysisProperties(String provider, URI baseUrl, String apiKey,
        String model, Duration connectTimeout, Duration readTimeout,
        int maxAttempts, long pollDelayMillis, long maxInputBytes) {
    public String providerOrDefault(){return provider==null?"disabled":provider;}
    public String modelOrDefault(){return model==null||model.isBlank()?"gpt-5.6":model;}
    public int maxAttemptsOrDefault(){return maxAttempts<=0?3:maxAttempts;}
    public long maxInputBytesOrDefault(){return maxInputBytes<=0?25L*1024*1024:maxInputBytes;}
}
