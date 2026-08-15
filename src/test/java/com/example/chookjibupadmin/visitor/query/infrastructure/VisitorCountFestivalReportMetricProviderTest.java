package com.example.chookjibupadmin.visitor.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class VisitorCountFestivalReportMetricProviderTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private VisitorCountFestivalReportMetricProvider provider;

    @Nested
    @DisplayName("findSummary")
    class FindSummary {

        @Test
        @DisplayName("총 방문 인원 수가 있으면 총계를 반환한다")
        void success_FindSummary_TotalPreferred() {
            // given
            LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 19, 12, 0);
            given(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                    .willReturn(List.of(Map.of(
                            "visitor_count", 30000,
                            "updated_at", Timestamp.valueOf(updatedAt)
                    )));

            // when
            Optional<FestivalReportMetricProvider.Snapshot> snapshot =
                    provider.findSummary(10L);

            // then
            assertThat(snapshot).isPresent();
            assertThat(snapshot.get().totalVisitorCount()).isEqualTo(30000L);
            assertThat(snapshot.get().generatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("총계가 없으면 일자별 합계를 반환한다")
        void success_FindSummary_DailyFallback() {
            // given
            LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 18, 21, 0);
            given(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                    .willReturn(List.of())
                    .willReturn(List.of(Map.of(
                            "row_count", 2,
                            "visitor_count", 4500,
                            "updated_at", Timestamp.valueOf(updatedAt)
                    )));

            // when
            Optional<FestivalReportMetricProvider.Snapshot> snapshot =
                    provider.findSummary(10L);

            // then
            assertThat(snapshot).isPresent();
            assertThat(snapshot.get().totalVisitorCount()).isEqualTo(4500L);
            assertThat(snapshot.get().generatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("방문 인원 데이터가 없으면 빈 값을 반환한다")
        void success_FindSummary_Empty() {
            // given
            given(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                    .willReturn(List.of())
                    .willReturn(List.of(Map.of(
                            "row_count", 0,
                            "visitor_count", 0
                    )));

            // when
            Optional<FestivalReportMetricProvider.Snapshot> snapshot =
                    provider.findSummary(10L);

            // then
            assertThat(snapshot).isEmpty();
        }
    }
}
