package com.example.chookjibupadmin.report.command.domain;

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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 결과 보고서 생성 작업을 저장하는 Aggregate이다.
 */
@Entity
@Getter
@Table(
        name = "festival_report_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_festival_report_job_public_id",
                columnNames = "public_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalReportJob extends BaseTimeEntity {

    private static final int MESSAGE_MAX_LENGTH = 1000;
    private static final int PROGRESS_MESSAGE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_job_id")
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;

    @Column(name = "schema_version", nullable = false, length = 30)
    private String schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FestivalReportJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "progress_day_index")
    private Integer progressDayIndex;

    @Column(name = "progress_message", length = 200)
    private String progressMessage;

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

    /**
     * 대기 상태의 결과 보고서 생성 작업을 만든다.
     */
    public static FestivalReportJob pending(
            Long festivalId,
            String provider,
            String model,
            String promptVersion,
            String schemaVersion
    ) {
        FestivalReportJob job = new FestivalReportJob();
        job.publicId = UUID.randomUUID();
        job.festivalId = festivalId;
        job.provider = provider;
        job.model = model;
        job.promptVersion = promptVersion;
        job.schemaVersion = schemaVersion;
        job.status = FestivalReportJobStatus.PENDING;
        return job;
    }

    /**
     * 대기 상태의 작업을 처리 중으로 전환한다.
     */
    public void start() {
        if (status != FestivalReportJobStatus.PENDING) {
            return;
        }

        status = FestivalReportJobStatus.PROCESSING;
        attemptCount++;
        startedAt = LocalDateTime.now();
        failureCode = null;
        failureMessage = null;
        nextAttemptAt = null;
    }

    /**
     * 처리 중인 작업의 일차별 진행 상황을 갱신한다.
     */
    public void updateProgress(Integer dayIndex, String message) {
        if (status != FestivalReportJobStatus.PROCESSING) {
            return;
        }

        progressDayIndex = dayIndex;
        progressMessage = limit(message, PROGRESS_MESSAGE_MAX_LENGTH);
    }

    /**
     * 처리 중인 작업을 완료 상태로 전환한다.
     */
    public void complete() {
        if (status != FestivalReportJobStatus.PROCESSING) {
            return;
        }

        status = FestivalReportJobStatus.COMPLETED;
        completedAt = LocalDateTime.now();
        nextAttemptAt = null;
        failureCode = null;
        failureMessage = null;
    }

    /**
     * 재시도 가능한 실패를 기록하고 다시 대기 상태로 되돌린다.
     */
    public void retry(String code, String message) {
        if (status != FestivalReportJobStatus.PROCESSING) {
            return;
        }

        status = FestivalReportJobStatus.PENDING;
        failureCode = code;
        failureMessage = limit(message, MESSAGE_MAX_LENGTH);
        nextAttemptAt = LocalDateTime.now().plusSeconds(
                Math.min(60L, 1L << Math.min(attemptCount, 5))
        );
    }

    /**
     * 더 이상 재시도하지 않는 실패로 종료한다.
     */
    public void fail(String code, String message) {
        if (status == FestivalReportJobStatus.CANCELLED) {
            return;
        }

        status = FestivalReportJobStatus.FAILED;
        failureCode = code;
        failureMessage = limit(message, MESSAGE_MAX_LENGTH);
        completedAt = LocalDateTime.now();
        nextAttemptAt = null;
    }

    /**
     * 아직 종료되지 않은 작업을 취소한다.
     */
    public void cancel() {
        if (!status.isActive()) {
            return;
        }

        status = FestivalReportJobStatus.CANCELLED;
        completedAt = LocalDateTime.now();
        nextAttemptAt = null;
    }

    private String limit(String message, int maxLength) {
        if (message == null) {
            return null;
        }

        return message.length() <= maxLength
                ? message
                : message.substring(0, maxLength);
    }
}
