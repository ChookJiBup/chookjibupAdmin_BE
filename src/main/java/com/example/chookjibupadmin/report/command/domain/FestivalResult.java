package com.example.chookjibupadmin.report.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 축제별 결과 보고서 스냅샷을 저장하는 Aggregate이다.
 */
@Entity
@Getter
@Table(name = "festival_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalResult {

    @Id
    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_json", columnDefinition = "jsonb")
    private String metricsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_json", columnDefinition = "jsonb")
    private String aiJson;

    @Column(name = "schema_version", length = 30)
    private String schemaVersion;

    @Column(name = "generation_status", length = 30)
    private String generationStatus;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /**
     * 새로운 결과 보고서 스냅샷을 생성한다.
     */
    public static FestivalResult create(
            Long festivalId,
            String metricsJson,
            String aiJson,
            String schemaVersion,
            String generationStatus
    ) {
        FestivalResult result = new FestivalResult();
        result.festivalId = festivalId;
        result.replace(metricsJson, aiJson, schemaVersion, generationStatus);
        return result;
    }

    /**
     * 기존 결과 보고서 스냅샷을 새 집계와 분석 결과로 교체한다.
     */
    public void replace(
            String metricsJson,
            String aiJson,
            String schemaVersion,
            String generationStatus
    ) {
        this.metricsJson = metricsJson;
        this.aiJson = aiJson;
        this.schemaVersion = schemaVersion;
        this.generationStatus = generationStatus;
        this.generatedAt = LocalDateTime.now();
    }
}
