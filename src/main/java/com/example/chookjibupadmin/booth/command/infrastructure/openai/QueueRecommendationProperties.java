package com.example.chookjibupadmin.booth.command.infrastructure.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 지도 이미지 분석의 활성화 여부와 독립적으로 줄 추천 제공자를 선택한다. */
@ConfigurationProperties(prefix = "app.booth.queue-recommendation")
public record QueueRecommendationProperties(String provider) {
    public String providerOrDefault() {
        return provider == null || provider.isBlank() ? "auto" : provider.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
