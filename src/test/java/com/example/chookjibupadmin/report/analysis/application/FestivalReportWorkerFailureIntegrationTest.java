package com.example.chookjibupadmin.report.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.report.analysis.application.port.FestivalReportAnalysisPort;
import com.example.chookjibupadmin.report.command.application.FestivalReportJobService;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
import com.example.chookjibupadmin.report.command.infrastructure.persistence.FestivalReportJobJpaRepository;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 분석 실패와 시간 초과가 «분석 중»에 머무르지 않고 사유와 함께 종료되는지 검증한다.
 */
@SpringBootTest
class FestivalReportWorkerFailureIntegrationTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 16);
    private static final LocalDate END_DATE = LocalDate.of(2026, 10, 18);

    @Autowired
    private FestivalReportWorker worker;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private FestivalVisitorCountService visitorCountService;

    @Autowired
    private FestivalReportJobService jobService;

    @Autowired
    private FestivalReportJobJpaRepository jobJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private FestivalReportAnalysisPort analysisPort;

    @BeforeEach
    void clear() {
        jobJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("재시도할 수 없는 분석 실패는 사유를 남기고 실패로 끝난다")
    void success_ProcessNext_AnalysisFailed() {
        // given
        Festival festival = persistFestivalWithVisitors();
        given(analysisPort.analyze(any(), any())).willThrow(
                new FestivalReportAnalysisException(
                        "OPENAI_INVALID_RESPONSE",
                        "분석 응답 형식이 올바르지 않습니다.",
                        false
                )
        );
        jobService.save(pending(festival.getId()));

        // when
        worker.processNext();

        // then
        FestivalReportJob job = jobService
                .findLatestByFestivalId(festival.getId())
                .orElseThrow();
        assertThat(job.getStatus()).isEqualTo(FestivalReportJobStatus.FAILED);
        assertThat(job.getFailureCode()).isEqualTo("OPENAI_INVALID_RESPONSE");
        assertThat(job.getFailureMessage())
                .isEqualTo("분석 응답 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("제한 시간을 넘겨 처리 중에 갇힌 작업을 회수한다")
    void success_ReleaseTimedOutJobs_StuckProcessing() {
        // given
        Festival festival = persistFestivalWithVisitors();
        jobService.save(pending(festival.getId()));
        UUID jobId = jobService.claimPending().orElseThrow().getPublicId();
        jdbcTemplate.update(
                "update festival_report_job set started_at = ? "
                        + "where public_id = ?",
                LocalDateTime.now().minusHours(1),
                jobId
        );

        // when
        worker.releaseTimedOutJobs();

        // then
        FestivalReportJob job = jobJpaRepository.findByPublicId(jobId)
                .orElseThrow();
        assertThat(job.getStatus())
                .isNotEqualTo(FestivalReportJobStatus.PROCESSING);
        assertThat(job.getFailureCode()).isEqualTo(
                FestivalReportJobService.TIMEOUT_FAILURE_CODE
        );
        assertThat(job.getFailureMessage()).isNotBlank();
    }

    private Festival persistFestivalWithVisitors() {
        Festival festival = festivalService.save(festival());
        LocalDate date = START_DATE;
        int count = 1000;
        while (!date.isAfter(END_DATE)) {
            visitorCountService.saveDaily(FestivalDailyVisitorCount.create(
                    festival.getId(),
                    date,
                    VisitorCount.of(count)
            ));
            date = date.plusDays(1);
            count += 500;
        }
        return festival;
    }

    private FestivalReportJob pending(Long festivalId) {
        return FestivalReportJob.pending(
                festivalId,
                "openai",
                "gpt-5.6",
                "1.0",
                "1.0"
        );
    }

    private Festival festival() {
        return Festival.create(
                ThreadLocalRandom.current().nextLong(100000L, 9000000L),
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(START_DATE, END_DATE),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
