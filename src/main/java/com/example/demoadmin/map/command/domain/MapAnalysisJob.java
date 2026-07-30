package com.example.demoadmin.map.command.domain;

import com.example.demoadmin.common.domain.BaseTimeEntity;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배치도 이미지 분석 작업의 상태를 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "map_analysis_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_map_analysis_jobs_public_id",
                        columnNames = "public_id"
                )
        }
)
public class MapAnalysisJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_map_id", nullable = false)
    private Long festivalMapId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MapAnalysisStatus status;

    @Column(name = "requested_by_admin_id", nullable = false)
    private Long requestedByAdminId;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    private MapAnalysisJob(
            Long festivalMapId,
            Long requestedByAdminId
    ) {
        validateId(festivalMapId);
        validateId(requestedByAdminId);

        this.publicId = UUID.randomUUID();
        this.festivalMapId = festivalMapId;
        this.requestedByAdminId = requestedByAdminId;
        this.status = MapAnalysisStatus.QUEUED;
    }

    public static MapAnalysisJob create(
            Long festivalMapId,
            Long requestedByAdminId
    ) {
        return new MapAnalysisJob(festivalMapId, requestedByAdminId);
    }

    public void start() {
        if (status != MapAnalysisStatus.QUEUED) {
            throw new CustomException(ErrorCode.MAP_ANALYSIS_STATUS_INVALID);
        }

        this.status = MapAnalysisStatus.ANALYZING;
    }

    public void complete() {
        if (status != MapAnalysisStatus.ANALYZING) {
            throw new CustomException(ErrorCode.MAP_ANALYSIS_STATUS_INVALID);
        }

        this.status = MapAnalysisStatus.COMPLETED;
        this.failureReason = null;
    }

    public void fail(String reason) {
        if (status != MapAnalysisStatus.QUEUED && status != MapAnalysisStatus.ANALYZING) {
            throw new CustomException(ErrorCode.MAP_ANALYSIS_STATUS_INVALID);
        }

        this.status = MapAnalysisStatus.FAILED;
        this.failureReason = normalizeReason(reason);
    }

    private static void validateId(Long id) {
        if (id == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "분석 작업에 실패했습니다.";
        }

        if (reason.length() > 1000) {
            return reason.substring(0, 1000);
        }

        return reason;
    }
}
