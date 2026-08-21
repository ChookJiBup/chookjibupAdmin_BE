package com.example.chookjibupadmin.report.analysis.infrastructure.openai;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(
        prefix = "app.report.analysis",
        name = "provider",
        havingValue = "openai"
)
public class OpenAiReportAnalysisConfig {

    @Bean
    @Qualifier("openAiReportRestClient")
    RestClient openAiReportRestClient(ReportAnalysisProperties properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("APP_OPENAI_API_KEY is required");
        }

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(
                properties.connectTimeout() == null
                        ? Duration.ofSeconds(5)
                        : properties.connectTimeout()
        );
        factory.setReadTimeout(
                properties.readTimeout() == null
                        ? Duration.ofSeconds(90)
                        : properties.readTimeout()
        );

        URI baseUrl = properties.baseUrl() == null
                ? URI.create("https://api.openai.com")
                : properties.baseUrl();

        return RestClient.builder()
                .baseUrl(baseUrl.toString())
                .requestFactory(factory)
                .defaultHeader(
                        "Authorization",
                        "Bearer " + properties.apiKey()
                )
                .build();
    }
}
