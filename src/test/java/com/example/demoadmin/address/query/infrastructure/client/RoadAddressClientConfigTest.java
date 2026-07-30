package com.example.demoadmin.address.query.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RoadAddressClientConfigTest {

    @Test
    @DisplayName("도로명주소 전용 RestClient를 생성한다")
    void success_RoadAddressRestClient() {
        // given
        RoadAddressClientConfig config = new RoadAddressClientConfig();
        RoadAddressProperties properties = new RoadAddressProperties(
                "https://business.juso.go.kr",
                "test-key",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );

        // when
        RestClient restClient = config.roadAddressRestClient(properties);

        // then
        assertThat(restClient).isNotNull();
    }
}
