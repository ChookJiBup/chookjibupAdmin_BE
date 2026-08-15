package com.example.chookjibupadmin.visitor.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalDailyVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalTotalVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.UpdateVisitorCountCommand;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Mock
    private FestivalOperationAccessService accessService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalVisitorCountService visitorCountService;

    @InjectMocks
    private FestivalVisitorCountApplicationService applicationService;

    @Nested
    @DisplayName("updateDailyVisitorCount")
    class UpdateDailyVisitorCount {

        @Test
        @DisplayName("일자별 방문 인원 수를 새로 저장한다")
        void success_UpdateDailyVisitorCount_Create() {
            // given
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 16);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            FestivalDailyVisitorCount saved = FestivalDailyVisitorCount.create(
                    10L,
                    visitDate,
                    VisitorCount.of(1200)
            );
            ReflectionTestUtils.setField(saved, "id", 100L);

            given(accessService.getAuthorizedFestivalId(publicId, principal))
                    .willReturn(10L);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdAndVisitDateForUpdate(
                    10L,
                    visitDate
            )).willReturn(Optional.empty());
            given(visitorCountService.saveDaily(any(FestivalDailyVisitorCount.class)))
                    .willReturn(saved);

            // when
            FestivalDailyVisitorCountResult result =
                    applicationService.updateDailyVisitorCount(
                            publicId,
                            visitDate,
                            new UpdateVisitorCountCommand(1200),
                            principal
                    );

            // then
            assertThat(result.festivalId()).isEqualTo(publicId);
            assertThat(result.visitDate()).isEqualTo(visitDate);
            assertThat(result.visitorCount()).isEqualTo(1200);
            then(visitorCountService).should().saveDaily(any(FestivalDailyVisitorCount.class));
        }

        @Test
        @DisplayName("기존 일자별 방문 인원 수를 수정한다")
        void success_UpdateDailyVisitorCount_UpdateExisting() {
            // given
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 16);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            FestivalDailyVisitorCount existing = FestivalDailyVisitorCount.create(
                    10L,
                    visitDate,
                    VisitorCount.of(100)
            );
            ReflectionTestUtils.setField(existing, "id", 100L);

            given(accessService.getAuthorizedFestivalId(publicId, principal))
                    .willReturn(10L);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdAndVisitDateForUpdate(
                    10L,
                    visitDate
            )).willReturn(Optional.of(existing));
            given(visitorCountService.saveDaily(existing)).willReturn(existing);

            // when
            FestivalDailyVisitorCountResult result =
                    applicationService.updateDailyVisitorCount(
                            publicId,
                            visitDate,
                            new UpdateVisitorCountCommand(500),
                            principal
                    );

            // then
            assertThat(result.visitorCount()).isEqualTo(500);
            assertThat(existing.getVisitorCountValue()).isEqualTo(500);
        }

        @Test
        @DisplayName("축제 기간 밖 일자는 입력할 수 없다")
        void fail_UpdateDailyVisitorCount_OutOfPeriod_CustomException() {
            // given
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            LocalDate visitDate = LocalDate.of(2026, 10, 20);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");

            given(accessService.getAuthorizedFestivalId(publicId, principal))
                    .willReturn(10L);
            given(festivalService.getById(10L)).willReturn(festival);

            // when & then
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
            // given
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            FestivalTotalVisitorCount saved = FestivalTotalVisitorCount.create(
                    10L,
                    VisitorCount.of(30000)
            );
            ReflectionTestUtils.setField(saved, "id", 200L);

            given(accessService.getAuthorizedFestivalId(publicId, principal))
                    .willReturn(10L);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findTotalByFestivalIdForUpdate(10L))
                    .willReturn(Optional.empty());
            given(visitorCountService.saveTotal(any(FestivalTotalVisitorCount.class)))
                    .willReturn(saved);

            // when
            FestivalTotalVisitorCountResult result =
                    applicationService.updateTotalVisitorCount(
                            publicId,
                            new UpdateVisitorCountCommand(30000),
                            principal
                    );

            // then
            assertThat(result.festivalId()).isEqualTo(publicId);
            assertThat(result.visitorCount()).isEqualTo(30000);
        }
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
