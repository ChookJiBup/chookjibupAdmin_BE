package com.example.chookjibupadmin.map.command.infrastructure.storage;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 배치도 저장용 AWS S3 Client를 구성한다.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "app.map.storage",
        name = "provider",
        havingValue = "s3"
)
public class S3MapStorageConfig {

    @Bean
    public S3Client mapS3Client(MapStorageProperties properties) {
        if (!properties.hasBucket()) {
            throw new IllegalStateException("APP_MAP_STORAGE_BUCKET is required");
        }
        Duration connectTimeout = properties.connectTimeout() == null
                ? Duration.ofSeconds(3)
                : properties.connectTimeout();
        Duration apiCallTimeout = properties.apiCallTimeout() == null
                ? Duration.ofSeconds(30)
                : properties.apiCallTimeout();
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(connectTimeout)
                        .socketTimeout(apiCallTimeout))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(apiCallTimeout)
                        .build())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(
                                properties.pathStyleAccessEnabled()
                        )
                        .build());
        if (properties.endpoint() != null) {
            builder.endpointOverride(properties.endpoint());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner mapS3Presigner(MapStorageProperties properties) {
        var builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(
                                properties.pathStyleAccessEnabled()
                        )
                        .build());
        if (properties.endpoint() != null) {
            builder.endpointOverride(properties.endpoint());
        }
        return builder.build();
    }
}
