package com.example.chookjibupadmin.map.analysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapAnalysisPropertiesTest {

    @Test
    @DisplayName("설정값이 비어 있으면 안전한 분석 기본값을 사용한다")
    void success_DefaultValues() {
        // given
        MapAnalysisProperties properties = new MapAnalysisProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                0
        );

        // when & then
        assertThat(properties.providerOrDefault()).isEqualTo("disabled");
        assertThat(properties.modelOrDefault()).isEqualTo("gpt-5.6");
        assertThat(properties.maxAttemptsOrDefault()).isEqualTo(3);
        assertThat(properties.maxInputBytesOrDefault())
                .isEqualTo(25L * 1024 * 1024);
    }
}
