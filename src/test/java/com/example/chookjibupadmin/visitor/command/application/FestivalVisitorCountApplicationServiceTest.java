package com.example.chookjibupadmin.visitor.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.command.application.FestivalReportGenerationApplicationService;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalDailyVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalTotalVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.UpdateVisitorCountCommand;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalVisitorCountApplicationServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalVisitorCountService visitorCountService;

    @Mock
    private FestivalReportGenerationApplicationService reportGenerationService;

    @Mock
    private Clock clock;

    @InjectMocks
    private FestivalVisitorCountApplicationService applicationService;

    private void givenClock(Instant instant) {
        Clock fixed = Clock.fixed(instant, SEOUL);
        given(clock.instant()).willReturn(fixed.instant());
        given(clock.getZone()).willReturn(fixed.getZone());
    }

    @Nested
    @DisplayName("updateDailyVisitorCount")
    class UpdateDailyVisitorCount {

        @Test
        @DisplayName("일자별 방문 인원 수를 새로 저장한다")
        void success_UpdateDailyVisitorCount_Create() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 16);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            AdminAccount admin = adminAccount(1L);
            FestivalDailyVisitorCount saved = FestivalDailyVisitorCount.create(
                    10L,
                    visitDate,
                    VisitorCount.of(1200)
            );
            ReflectionTestUtils.setField(saved, "id", 100L);

            givenClock(Instant.parse("2026-10-16T15:00:00Z"));
            given(adminAccountService.getById(1L)).willReturn(admin);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(AdminFestivalRole.createFestivalOwner(1L, 10L));
            given(visitorCountService.findDailyByFestivalIdAndVisitDateForUpdate(
                    10L,
                    visitDate
            )).willReturn(Optional.empty());
            given(visitorCountService.saveDaily(any(FestivalDailyVisitorCount.class)))
                    .willReturn(saved);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(saved));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.empty());

            FestivalDailyVisitorCountResult result =
                    applicationService.updateDailyVisitorCount(
                            publicId,
                            visitDate,
                            new UpdateVisitorCountCommand(1200),
                            principal
                    );

            assertThat(result.festivalId()).isEqualTo(publicId);
            assertThat(result.visitDate()).isEqualTo(visitDate);
            assertThat(result.visitorCount()).isEqualTo(1200);
            assertThat(result.allDaysFilled()).isFalse();
            assertThat(result.reportReadyToGenerate()).isFalse();
            then(visitorCountService).should(never())
                    .saveTotal(any(FestivalTotalVisitorCount.class));
            then(reportGenerationService).should(never()).enqueueIfReady(10L);
        }

        @Test
        @DisplayName("모든 일자를 채우면 총원을 덮어쓰지 않고 리포트 생성을 큐에 넣는다")
        void success_UpdateDailyVisitorCount_EnqueueWhenAllFilled() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 18);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            AdminAccount admin = adminAccount(1L);
            FestivalDailyVisitorCount day1 = FestivalDailyVisitorCount.create(
                    10L,
                    LocalDate.of(2026, 10, 16),
                    VisitorCount.of(100)
            );
            FestivalDailyVisitorCount day2 = FestivalDailyVisitorCount.create(
                    10L,
                    LocalDate.of(2026, 10, 17),
                    VisitorCount.of(200)
            );
            FestivalDailyVisitorCount day3 = FestivalDailyVisitorCount.create(
                    10L,
                    visitDate,
                    VisitorCount.of(300)
            );
            ReflectionTestUtils.setField(day3, "id", 103L);

            givenClock(Instant.parse("2026-10-18T15:00:00Z"));
            given(adminAccountService.getById(1L)).willReturn(admin);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(AdminFestivalRole.createFestivalOwner(1L, 10L));
            given(visitorCountService.findDailyByFestivalIdAndVisitDateForUpdate(
                    10L,
                    visitDate
            )).willReturn(Optional.empty());
            given(visitorCountService.saveDaily(any(FestivalDailyVisitorCount.class)))
                    .willReturn(day3);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(day1, day2, day3));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.empty());

            FestivalDailyVisitorCountResult result =
                    applicationService.updateDailyVisitorCount(
                            publicId,
                            visitDate,
                            new UpdateVisitorCountCommand(300),
                            principal
                    );

            assertThat(result.allDaysFilled()).isTrue();
            assertThat(result.reportReadyToGenerate()).isTrue();
            then(visitorCountService).should(never())
                    .saveTotal(any(FestivalTotalVisitorCount.class));
            then(reportGenerationService).should().enqueueIfReady(10L);
        }

        @Test
        @DisplayName("오늘 이후 일자는 입력할 수 없다")
        void fail_UpdateDailyVisitorCount_FutureOrToday_CustomException() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 17);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            AdminAccount admin = adminAccount(1L);

            givenClock(Instant.parse("2026-10-16T15:00:00Z"));
            given(adminAccountService.getById(1L)).willReturn(admin);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(AdminFestivalRole.createFestivalOwner(1L, 10L));

            assertThatThrownBy(() -> applicationService.updateDailyVisitorCount(
                    publicId,
                    visitDate,
                    new UpdateVisitorCountCommand(100),
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 기간 밖 일자는 입력할 수 없다")
        void fail_UpdateDailyVisitorCount_OutOfPeriod_CustomException() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 20);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            AdminAccount admin = adminAccount(1L);

            given(adminAccountService.getById(1L)).willReturn(admin);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(AdminFestivalRole.createFestivalOwner(1L, 10L));

            assertThatThrownBy(() -> applicationService.updateDailyVisitorCount(
                    publicId,
                    visitDate,
                    new UpdateVisitorCountCommand(100),
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("updateTotalVisitorCount")
    class UpdateTotalVisitorCount {

        @Test
        @DisplayName("총 방문 인원 수를 저장한다")
        void success_UpdateTotalVisitorCount_Create() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            AdminAccount admin = adminAccount(1L);
            FestivalTotalVisitorCount saved = FestivalTotalVisitorCount.create(
                    10L,
                    VisitorCount.of(30000)
            );
            ReflectionTestUtils.setField(saved, "id", 200L);

            given(adminAccountService.getById(1L)).willReturn(admin);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(AdminFestivalRole.createFestivalOwner(1L, 10L));
            given(visitorCountService.findTotalByFestivalIdForUpdate(10L))
                    .willReturn(Optional.empty());
            given(visitorCountService.saveTotal(any(FestivalTotalVisitorCount.class)))
                    .willReturn(saved);

            FestivalTotalVisitorCountResult result =
                    applicationService.updateTotalVisitorCount(
                            publicId,
                            new UpdateVisitorCountCommand(30000),
                            principal
                    );

            assertThat(result.festivalId()).isEqualTo(publicId);
            assertThat(result.visitorCount()).isEqualTo(30000);
            then(reportGenerationService).should().enqueueIfReady(10L);
        }
    }

    private AdminAccount adminAccount(Long adminId) {
        AdminAccount admin = AdminAccount.createAdmin(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("관리자"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("hash")
        );
        ReflectionTestUtils.setField(admin, "id", adminId);
        return admin;
    }

    private Festival festival(Long festivalId) {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
        ReflectionTestUtils.setField(festival, "id", festivalId);
        return festival;
    }
}
