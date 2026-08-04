package com.example.chookjibupadmin.map.analysis.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "map_analysis_job", uniqueConstraints =
        @UniqueConstraint(name = "uk_map_analysis_job_public_id", columnNames = "public_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapAnalysisJob extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;
    @Column(name = "map_id", nullable = false, updatable = false)
    private Long mapId;
    @Column(nullable = false, length = 30)
    private String provider;
    @Column(nullable = false, length = 100)
    private String model;
    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;
    @Column(name = "schema_version", nullable = false, length = 30)
    private String schemaVersion;
    @Column(name = "input_image_key", nullable = false, length = 700)
    private String inputImageKey;
    @Column(name = "input_checksum_sha256", nullable = false, length = 64)
    private String inputChecksumSha256;
    @Column(name = "input_image_width", nullable = false)
    private int inputImageWidth;
    @Column(name = "input_image_height", nullable = false)
    private int inputImageHeight;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private MapAnalysisJobStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "detected_count", nullable = false)
    private int detectedCount;
    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;
    @Column(name = "rejected_count", nullable = false)
    private int rejectedCount;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rejected_details", nullable = false, columnDefinition = "jsonb")
    private String rejectedDetails;
    @Column(name = "failure_code", length = 100)
    private String failureCode;
    @Column(name = "failure_message", length = 1000)
    private String failureMessage;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;
    @Version
    private Long version;

    public static MapAnalysisJob pending(Long mapId, String provider, String model,
            String inputImageKey, String checksum, int width, int height) {
        MapAnalysisJob job = new MapAnalysisJob();
        job.publicId = UUID.randomUUID();
        job.mapId = mapId;
        job.provider = provider;
        job.model = model;
        job.promptVersion = "1.0";
        job.schemaVersion = "1.0";
        job.inputImageKey = inputImageKey;
        job.inputChecksumSha256 = checksum;
        job.inputImageWidth = width;
        job.inputImageHeight = height;
        job.status = MapAnalysisJobStatus.PENDING;
        job.rejectedDetails = "[]";
        return job;
    }

    public void start() {
        if (status != MapAnalysisJobStatus.PENDING) return;
        status = MapAnalysisJobStatus.PROCESSING;
        attemptCount++;
        startedAt = LocalDateTime.now();
        failureCode = null;
        failureMessage = null;
        nextAttemptAt = null;
    }

    public void complete(int detected, int accepted, int rejected, String rejectedDetails) {
        if (status != MapAnalysisJobStatus.PROCESSING) return;
        this.detectedCount = detected;
        this.acceptedCount = accepted;
        this.rejectedCount = rejected;
        this.rejectedDetails = rejectedDetails == null ? "[]" : rejectedDetails;
        status = MapAnalysisJobStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public void retry(String code, String message) {
        if (status != MapAnalysisJobStatus.PROCESSING) return;
        status = MapAnalysisJobStatus.PENDING;
        failureCode = code;
        failureMessage = limit(message);
        nextAttemptAt = LocalDateTime.now().plusSeconds(
                Math.min(60L, 1L << Math.min(attemptCount, 5))
        );
    }

    public void fail(String code, String message) {
        if (status == MapAnalysisJobStatus.CANCELLED) return;
        status = MapAnalysisJobStatus.FAILED;
        failureCode = code;
        failureMessage = limit(message);
        completedAt = LocalDateTime.now();
        nextAttemptAt = null;
    }

    public void cancel() {
        if (status == MapAnalysisJobStatus.PENDING || status == MapAnalysisJobStatus.PROCESSING) {
            status = MapAnalysisJobStatus.CANCELLED;
            completedAt = LocalDateTime.now();
            nextAttemptAt = null;
        }
    }

    private String limit(String message) {
        if (message == null) return null;
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
