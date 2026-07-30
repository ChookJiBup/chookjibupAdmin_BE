package com.example.demoadmin.address.query.infrastructure.client;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 공식 도로명주소 검색 API용 HTTP client를 구성한다.
 */
@Configuration
public class RoadAddressClientConfig {

    /**
     * 연결·응답 timeout을 적용한 도로명주소 전용 RestClient를 제공한다.
     */
    @Bean
    @Qualifier("roadAddressRestClient")
    public RestClient roadAddressRestClient(RoadAddressProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
