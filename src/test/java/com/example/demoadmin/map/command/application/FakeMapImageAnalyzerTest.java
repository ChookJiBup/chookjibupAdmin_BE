package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.example.demoadmin.map.command.infrastructure.fake.FakeMapImageAnalyzer;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FakeMapImageAnalyzerTest {

    private final FakeMapImageAnalyzer analyzer = new FakeMapImageAnalyzer();

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("테스트용 지도 객체를 반환한다")
        void success_Analyze() {
            // given
            MapImageAnalysisRequest request = new MapImageAnalysisRequest(
                    UUID.randomUUID(),
                    "images/김밥축제_지적편집도.png",
                    MapStorageType.TEST_RESOURCE,
                    1745,
                    1577
            );

            // when
            var result = analyzer.analyze(request);

            // then
            assertThat(result.objects()).hasSize(2);
        }
    }
}
