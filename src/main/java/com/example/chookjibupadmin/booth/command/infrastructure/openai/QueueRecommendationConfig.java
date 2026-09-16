package com.example.chookjibupadmin.booth.command.infrastructure.openai;

import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class QueueRecommendationConfig {
    @Bean
    QueuePlanRecommendationPort queuePlanRecommendationPort(QueueRecommendationProperties queue,
            MapAnalysisProperties shared, ObjectMapper mapper) {
        String provider = queue.providerOrDefault();
        if (!java.util.Set.of("auto", "openai", "disabled").contains(provider)) {
            throw new IllegalStateException("APP_QUEUE_RECOMMENDATION_PROVIDER must be auto, openai or disabled");
        }
        if ("disabled".equals(provider)) return new DisabledQueuePlanRecommendationAdapter();
        if (shared.apiKey() == null || shared.apiKey().isBlank()) {
            return new DisabledQueuePlanRecommendationAdapter("서버에 AI 인증 설정이 없습니다. 운영자가 APP_OPENAI_API_KEY를 설정해야 합니다.");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(shared.connectTimeout() == null ? Duration.ofSeconds(5) : shared.connectTimeout());
        factory.setReadTimeout(shared.readTimeout() == null ? Duration.ofSeconds(90) : shared.readTimeout());
        var client = RestClient.builder()
                .baseUrl(shared.baseUrl() == null ? "https://api.openai.com" : shared.baseUrl().toString())
                .requestFactory(factory).defaultHeader("Authorization", "Bearer " + shared.apiKey()).build();
        return new OpenAiQueuePlanRecommendationAdapter(client, mapper, shared);
    }
}
