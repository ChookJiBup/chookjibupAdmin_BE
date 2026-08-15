package com.example.chookjibupadmin.visitor.query.infrastructure;

import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 수동 입력된 방문 인원 수를 축제 결과 보고서 지표로 제공한다.
 */
@Repository
@RequiredArgsConstructor
public class VisitorCountFestivalReportMetricProvider
        implements FestivalReportMetricProvider {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<Snapshot> findSummary(Long festivalId) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource("festivalId", festivalId);

        List<Map<String, Object>> totalRows = jdbcTemplate.queryForList(
                """
                    select total_visitor_count as visitor_count,
                           updated_at as updated_at
                    from festival_visitor_total
                    where festival_id = :festivalId
                """,
                parameters
        );
        if (!totalRows.isEmpty()) {
            Map<String, Object> row = totalRows.get(0);
            return Optional.of(toSnapshot(
                    ((Number) row.get("visitor_count")).longValue(),
                    row.get("updated_at")
            ));
        }

        List<Map<String, Object>> dailyRows = jdbcTemplate.queryForList(
                """
                    select count(*) as row_count,
                           coalesce(sum(visitor_count), 0) as visitor_count,
                           max(updated_at) as updated_at
                    from festival_visitor_count
                    where festival_id = :festivalId
                """,
                parameters
        );
        Map<String, Object> dailyRow = dailyRows.get(0);
        if (((Number) dailyRow.get("row_count")).longValue() == 0L) {
            return Optional.empty();
        }

        return Optional.of(toSnapshot(
                ((Number) dailyRow.get("visitor_count")).longValue(),
                dailyRow.get("updated_at")
        ));
    }

    private Snapshot toSnapshot(
            long visitorCount,
            Object updatedAt
    ) {
        return new Snapshot(
                visitorCount,
                0L,
                0L,
                toLocalDateTime(updatedAt)
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }
}
