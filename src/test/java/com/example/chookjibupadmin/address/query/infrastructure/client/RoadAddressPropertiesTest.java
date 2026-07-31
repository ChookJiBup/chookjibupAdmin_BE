package com.example.chookjibupadmin.address.query.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoadAddressPropertiesTest {

    @Nested
    @DisplayName("hasConfirmationKey")
    class HasConfirmationKey {

        @Test
        @DisplayName("승인키가 설정되어 있으면 true를 반환한다")
        void success_HasConfirmationKey() {
            // given
            RoadAddressProperties properties = properties("test-key");

            // when
            boolean result = properties.hasConfirmationKey();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("승인키가 공백이면 false를 반환한다")
        void success_HasConfirmationKey_BlankBoundary() {
            // given
            RoadAddressProperties properties = properties(" ");

            // when
            boolean result = properties.hasConfirmationKey();

            // then
            assertThat(result).isFalse();
        }
    }

    private RoadAddressProperties properties(String confirmationKey) {
        return new RoadAddressProperties(
                "https://business.juso.go.kr",
                confirmationKey,
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );
    }
}
