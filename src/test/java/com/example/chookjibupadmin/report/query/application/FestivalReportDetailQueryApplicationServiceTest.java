package com.example.chookjibupadmin.report.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.report.command.application.FestivalReportJobService;
import com.example.chookjibupadmin.report.command.application.FestivalResultService;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalResult;
import com.example.chookjibupadmin.report.query.application.dto.FestivalReportStatusView;
import com.example.chookjibupadmin.report.query.infrastructure.FestivalReviewMetricQueryRepository;
import com.example.chookjibupadmin.report.support.FestivalReportMetricAssembler;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalReportDetailQueryApplicationServiceTest {

    @InjectMocks
    private FestivalReportDetailQueryApplicationService service;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalVisitorCountService visitorCountService;

    @Mock
    private FestivalReportJobService reportJobService;

    @Mock
    private FestivalResultService resultService;

    @Mock
    private FestivalReportMetricAssembler metricAssembler;

    @Mock
    private FestivalReviewMetricQueryRepository reviewMetricQueryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Clock clock;

    @Test
    @DisplayName("방문 입력만 완료되면 performance·evaluation 모두 false다")
    void success_GetStatus_VisitorReadyAlone_NotAvailable() {
        Festival festival = festival();
        stubAuth(festival);
        given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                .willReturn(completeDaily());
        given(visitorCountService.findTotalByFestivalId(10L)).willReturn(Optional.empty());
        given(reportJobService.findLatestByFestivalId(10L)).willReturn(Optional.empty());
        given(resultService.findByFestivalId(10L)).willReturn(Optional.empty());

        FestivalReportStatusView view = service.getStatus(
                festival.getPublicId(),
                new AdminPrincipal(1L, "hong@korea.kr")
        );

        assertThat(view.performanceAvailable()).isFalse();
        assertThat(view.evaluationAvailable()).isFalse();
    }

    @Test
    @DisplayName("결과가 있으면 performance만 true이고 evaluation은 AI 전까지 false다")
    void success_GetStatus_ResultWithoutEvaluationAi() {
        Festival festival = festival();
        stubAuth(festival);
        FestivalResult result = FestivalResult.create(
                10L,
                "{}",
                "",
                "1.0",
                "COMPLETED"
        );
        given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                .willReturn(completeDaily());
        given(visitorCountService.findTotalByFestivalId(10L)).willReturn(Optional.empty());
        given(reportJobService.findLatestByFestivalId(10L)).willReturn(Optional.empty());
        given(resultService.findByFestivalId(10L)).willReturn(Optional.of(result));

        FestivalReportStatusView view = service.getStatus(
                festival.getPublicId(),
                new AdminPrincipal(1L, "hong@korea.kr")
        );

        assertThat(view.performanceAvailable()).isTrue();
        assertThat(view.evaluationAvailable()).isFalse();
    }

    @Test
    @DisplayName("job FAILED면 performance·evaluation 모두 false다")
    void success_GetStatus_FailedJob() {
        Festival festival = festival();
        stubAuth(festival);
        FestivalReportJob job = FestivalReportJob.pending(
                10L,
                "openai",
                "gpt-5.6",
                "1.0",
                "1.0"
        );
        job.start();
        job.fail("OPENAI_ERROR", "failed");
        FestivalResult result = FestivalResult.create(
                10L,
                "{}",
                "",
                "1.0",
                "FAILED"
        );
        given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                .willReturn(completeDaily());
        given(visitorCountService.findTotalByFestivalId(10L)).willReturn(Optional.empty());
        given(reportJobService.findLatestByFestivalId(10L)).willReturn(Optional.of(job));
        given(resultService.findByFestivalId(10L)).willReturn(Optional.of(result));

        FestivalReportStatusView view = service.getStatus(
                festival.getPublicId(),
                new AdminPrincipal(1L, "hong@korea.kr")
        );

        assertThat(view.performanceAvailable()).isFalse();
        assertThat(view.evaluationAvailable()).isFalse();
    }

    private void stubAuth(Festival festival) {
        AdminAccount admin = admin();
        given(clock.instant()).willReturn(Instant.parse("2026-10-20T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 10L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 10L));
    }

    private AdminAccount admin() {
        AdminAccount account = AdminAccount.createAdmin(
                AdminEmail.of("hong@korea.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("hash")
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }

    private Festival festival() {
        Festival festival = Festival.create(
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울"),
                FestivalDetailAddress.of(null),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0)),
                FestivalVisitorCountInputMode.DAILY
        );
        ReflectionTestUtils.setField(festival, "id", 10L);
        return festival;
    }

    private List<FestivalDailyVisitorCount> completeDaily() {
        return List.of(
                FestivalDailyVisitorCount.create(10L, LocalDate.of(2026, 10, 16), VisitorCount.of(100)),
                FestivalDailyVisitorCount.create(10L, LocalDate.of(2026, 10, 17), VisitorCount.of(200)),
                FestivalDailyVisitorCount.create(10L, LocalDate.of(2026, 10, 18), VisitorCount.of(300))
        );
    }
}
