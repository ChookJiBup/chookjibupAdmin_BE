package com.example.chookjibupadmin.report.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.report.command.application.FestivalReportJobService;
import com.example.chookjibupadmin.report.command.application.FestivalResultService;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
import com.example.chookjibupadmin.report.command.infrastructure.persistence.FestivalReportJobJpaRepository;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 여러 일차의 진행 상황을 갱신하는 실제 파이프라인이 끝까지 도달하는지 검증한다.
 */
@SpringBootTest
class FestivalReportWorkerIntegrationTest {

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
    private FestivalResultService resultService;

    @Autowired
    private FestivalReportJobJpaRepository jobJpaRepository;

    @BeforeEach
    void clear() {
        jobJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("여러 일차를 가진 축제의 보고서 작업이 완료 상태로 끝난다")
    void success_ProcessNext_MultiDayFestival() {
        // given
        Festival festival = festivalService.save(festival());
        persistDailyVisitorCounts(festival.getId());
        jobService.save(FestivalReportJob.pending(
                festival.getId(),
                "disabled",
                "gpt-5.6",
                "1.0",
                "1.0"
        ));

        // when
        worker.processNext();

        // then
        FestivalReportJob job = jobService
                .findLatestByFestivalId(festival.getId())
                .orElseThrow();
        assertThat(job.getStatus())
                .isEqualTo(FestivalReportJobStatus.COMPLETED);
        assertThat(job.getFailureCode()).isNull();
        assertThat(job.getProgressDayIndex()).isEqualTo(3);
        assertThat(resultService.findByFestivalId(festival.getId()))
                .isPresent();
    }

    private void persistDailyVisitorCounts(Long festivalId) {
        LocalDate date = START_DATE;
        int count = 1000;
        while (!date.isAfter(END_DATE)) {
            visitorCountService.saveDaily(FestivalDailyVisitorCount.create(
                    festivalId,
                    date,
                    VisitorCount.of(count)
            ));
            date = date.plusDays(1);
            count += 500;
        }
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
