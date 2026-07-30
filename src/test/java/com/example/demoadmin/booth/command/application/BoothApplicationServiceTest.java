package com.example.demoadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.command.application.dto.CreateBoothCommand;
import com.example.demoadmin.booth.command.application.dto.CreateBoothQueueLineCommand;
import com.example.demoadmin.booth.command.application.dto.BoothQueueTailResult;
import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueLineCommand;
import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueTailCommand;
import com.example.demoadmin.booth.command.domain.BoothOperatingStatus;
import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.operator.command.application.FieldStaffAccountService;
import com.example.demoadmin.operator.command.domain.FieldStaffAccount;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffName;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.demoadmin.operator.support.FieldStaffPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.LocalTime;
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
class BoothApplicationServiceTest {

    @InjectMocks
    private BoothApplicationService boothApplicationService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalBoothService festivalBoothService;

    @Mock
    private BoothQueueLineService boothQueueLineService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-29T03:00:00Z"),
            ZoneOffset.UTC
    );

    @org.junit.jupiter.api.BeforeEach
    void setUpClock() {
        ReflectionTestUtils.setField(boothApplicationService, "clock", clock);
    }

    @Nested
    @DisplayName("createBooth")
    class CreateBooth {

        @Test
        @DisplayName("1관리자는 축제 부스를 생성한다")
        void success_CreateBooth_FestivalOwner() {
            // given
            Festival festival = festival(1L);
            AdminPrincipal principal = principal();
            CreateBoothCommand command = createBoothCommand();
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    principal.adminId(),
                    festival.getId()
            )).willReturn(AdminFestivalRole.createFestivalOwner(principal.adminId(), festival.getId()));
            given(festivalBoothService.save(any(FestivalBooth.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            FestivalBooth booth = boothApplicationService.createBooth(
                    festival.getPublicId(),
                    command,
                    principal
            );

            // then
            assertThat(booth.getFestivalId()).isEqualTo(festival.getId());
            assertThat(booth.getNameValue()).isEqualTo(command.name());
            then(festivalBoothService).should().save(any(FestivalBooth.class));
        }
    }

    @Nested
    @DisplayName("createQueueLine")
    class CreateQueueLine {

        @Test
        @DisplayName("1관리자는 부스 대기 라인을 생성한다")
        void success_CreateQueueLine_FestivalOwner() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            AdminPrincipal principal = principal();
            CreateBoothQueueLineCommand command = createQueueLineCommand(1);
            givenManagedBooth(festival, booth, principal);
            given(boothQueueLineService.existsByBoothIdAndLineOrder(booth.getId(), 1))
                    .willReturn(false);
            given(boothQueueLineService.save(any(BoothQueueLine.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            BoothQueueLine queueLine = boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    command,
                    principal
            );

            // then
            assertThat(queueLine.getBoothId()).isEqualTo(booth.getId());
            assertThat(queueLine.getLineOrder()).isOne();
            then(boothQueueLineService).should().save(any(BoothQueueLine.class));
        }

        @Test
        @DisplayName("같은 부스에 같은 순서의 대기 라인을 생성할 수 없다")
        void fail_CreateQueueLine_CustomException_DuplicatedLineOrder() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            AdminPrincipal principal = principal();
            CreateBoothQueueLineCommand command = createQueueLineCommand(1);
            givenManagedBooth(festival, booth, principal);
            given(boothQueueLineService.existsByBoothIdAndLineOrder(booth.getId(), 1))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_QUEUE_LINE_ORDER_DUPLICATED.getMessage());
        }
    }

    @Nested
    @DisplayName("updateQueueLine")
    class UpdateQueueLine {

        @Test
        @DisplayName("현재 줄 끝 라인의 대기시간을 수정하면 부스 대기시간도 갱신한다")
        void success_UpdateQueueLine_CurrentQueueTail() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId());
            booth.updateQueueTail(queueLine);
            AdminPrincipal principal = principal();
            givenManagedBooth(festival, booth, principal);
            given(boothQueueLineService.getByBoothIdAndPublicId(
                    booth.getId(),
                    queueLine.getPublicId()
            )).willReturn(queueLine);

            // when
            BoothQueueLine result = boothApplicationService.updateQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    queueLine.getPublicId(),
                    new UpdateBoothQueueLineCommand(
                            1,
                            "수정 라인",
                            25,
                            100,
                            "{}",
                            "{}"
                    ),
                    principal
            );

            // then
            assertThat(result.getExpectedWaitingMinutes()).isEqualTo(25);
            assertThat(booth.getExpectedWaitingMinutes()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("updateQueueTail")
    class UpdateQueueTail {

        @Test
        @DisplayName("서브관리자는 현재 줄 끝을 갱신한다")
        void success_UpdateQueueTail_SubAdmin() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId());
            AdminPrincipal principal = principal();
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    principal.adminId(),
                    festival.getId()
            )).willReturn(AdminFestivalRole.createSubAdmin(principal.adminId(), festival.getId(), 99L));
            given(festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                    festival.getId(),
                    booth.getPublicId()
            ))
                    .willReturn(booth);
            given(boothQueueLineService.getByBoothIdAndPublicId(booth.getId(), queueLine.getPublicId()))
                    .willReturn(queueLine);

            // when
            BoothQueueTailResult result = boothApplicationService.updateQueueTail(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    new UpdateBoothQueueTailCommand(queueLine.getPublicId(), "OPERATING"),
                    principal
            );

            // then
            FestivalBooth updated = result.booth();
            assertThat(updated.getCurrentQueueLineId()).isEqualTo(queueLine.getId());
            assertThat(updated.getOperatingStatus()).isEqualTo(BoothOperatingStatus.OPERATING);
            assertThat(result.currentQueueLine()).isEqualTo(queueLine);
        }

        @Test
        @DisplayName("마감 상태로 갱신하면 대기 라인 ID 없이 부스를 마감한다")
        void success_UpdateQueueTail_Closed() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            AdminPrincipal principal = principal();
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    principal.adminId(),
                    festival.getId()
            )).willReturn(AdminFestivalRole.createSubAdmin(principal.adminId(), festival.getId(), 99L));
            given(festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                    festival.getId(),
                    booth.getPublicId()
            ))
                    .willReturn(booth);

            // when
            BoothQueueTailResult result = boothApplicationService.updateQueueTail(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    new UpdateBoothQueueTailCommand(null, "CLOSED"),
                    principal
            );

            // then
            FestivalBooth updated = result.booth();
            assertThat(updated.getOperatingStatus()).isEqualTo(BoothOperatingStatus.CLOSED);
            assertThat(updated.getCurrentQueueLineId()).isNull();
            assertThat(updated.getExpectedWaitingMinutes()).isZero();
            assertThat(result.currentQueueLine()).isNull();
        }

        @Test
        @DisplayName("유효한 현장 스태프는 현재 줄 끝을 갱신한다")
        void success_UpdateQueueTail_FieldStaff() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId());
            FieldStaffAccount account = fieldStaffAccount(30L, festival.getId());
            FieldStaffPrincipal principal = new FieldStaffPrincipal(
                    account.getId(),
                    festival.getId(),
                    account.getLoginIdValue()
            );
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getById(account.getId()))
                    .willReturn(account);
            given(festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                    festival.getId(),
                    booth.getPublicId()
            )).willReturn(booth);
            given(boothQueueLineService.getByBoothIdAndPublicId(
                    booth.getId(),
                    queueLine.getPublicId()
            )).willReturn(queueLine);

            // when
            BoothQueueTailResult result = boothApplicationService.updateQueueTail(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    new UpdateBoothQueueTailCommand(queueLine.getPublicId(), "OPERATING"),
                    principal
            );

            // then
            assertThat(result.booth().getCurrentQueueLineId()).isEqualTo(queueLine.getId());
            assertThat(result.currentQueueLine()).isEqualTo(queueLine);
        }

        @Test
        @DisplayName("토큰 로그인 ID와 계정 로그인 ID가 다르면 줄 끝을 갱신할 수 없다")
        void fail_UpdateQueueTail_CustomException_FieldStaffLoginIdMismatch() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(30L, festival.getId());
            FieldStaffPrincipal principal = new FieldStaffPrincipal(
                    account.getId(),
                    festival.getId(),
                    "changed-login-id"
            );
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getById(account.getId()))
                    .willReturn(account);

            // when & then
            assertThatThrownBy(() -> boothApplicationService.updateQueueTail(
                    festival.getPublicId(),
                    UUID.randomUUID(),
                    new UpdateBoothQueueTailCommand(null, "CLOSED"),
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("마감 외 상태는 대기 라인 ID가 필요하다")
        void fail_UpdateQueueTail_CustomException_QueueLineIdNull() {
            // given
            Festival festival = festival(1L);
            FestivalBooth booth = savedBooth(10L, festival.getId());
            AdminPrincipal principal = principal();
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    principal.adminId(),
                    festival.getId()
            )).willReturn(AdminFestivalRole.createSubAdmin(principal.adminId(), festival.getId(), 99L));
            given(festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                    festival.getId(),
                    booth.getPublicId()
            ))
                    .willReturn(booth);

            // when & then
            assertThatThrownBy(() -> boothApplicationService.updateQueueTail(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    new UpdateBoothQueueTailCommand(null, "OPERATING"),
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    private void givenManagedBooth(
            Festival festival,
            FestivalBooth booth,
            AdminPrincipal principal
    ) {
        given(festivalService.getByPublicId(festival.getPublicId()))
                .willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                principal.adminId(),
                festival.getId()
        )).willReturn(AdminFestivalRole.createFestivalOwner(principal.adminId(), festival.getId()));
        given(festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                festival.getId(),
                booth.getPublicId()
        ))
                .willReturn(booth);
    }

    private CreateBoothCommand createBoothCommand() {
        return new CreateBoothCommand(
                "푸드 부스",
                "먹거리",
                "A-1",
                "대표 먹거리 부스"
        );
    }

    private CreateBoothQueueLineCommand createQueueLineCommand(int lineOrder) {
        return new CreateBoothQueueLineCommand(
                lineOrder,
                "첫 번째 라인",
                10,
                100,
                "{}",
                "{}"
        );
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "owner@mapo.go.kr");
    }

    private Festival festival(Long id) {
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
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }

    private FestivalBooth savedBooth(
            Long id,
            Long festivalId
    ) {
        FestivalBooth booth = FestivalBooth.create(
                festivalId,
                BoothName.of("푸드 부스"),
                BoothCategory.of("먹거리"),
                BoothLocation.of("A-1"),
                BoothDescription.of("대표 먹거리 부스")
        );
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private BoothQueueLine savedQueueLine(
            Long id,
            Long boothId
    ) {
        BoothQueueLine queueLine = BoothQueueLine.create(
                boothId,
                1,
                BoothLineLabel.of("첫 번째 라인"),
                10,
                100,
                "{}",
                "{}"
        );
        ReflectionTestUtils.setField(queueLine, "id", id);
        return queueLine;
    }

    private FieldStaffAccount fieldStaffAccount(
            Long id,
            Long festivalId
    ) {
        FieldStaffAccount account = FieldStaffAccount.create(
                festivalId,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("현장 스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("$2a$10$testPasswordHashValue"),
                java.time.LocalDateTime.of(2026, 7, 28, 0, 0),
                java.time.LocalDateTime.of(2026, 7, 30, 23, 59)
        );
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
