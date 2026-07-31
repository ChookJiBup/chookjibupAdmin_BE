package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.UpdateFestivalCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalApplicationServiceTest {

    @InjectMocks
    private FestivalApplicationService festivalApplicationService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalSeriesService festivalSeriesService;

    @Mock
    private AdminAccountService adminAccountService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("축제 기본 정보를 저장하고 생성자를 1관리자로 배정한다")
        void success_Create_AssignFestivalOwner() {
            // given
            CreateFestivalCommand command = createCommand();
            AdminAccount adminAccount = unassignedAdmin();
            AdminPrincipal principal = principal(null, null);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026))
                    .willReturn(false);
            given(festivalService.save(any(Festival.class)))
                    .willAnswer(invocation -> {
                        Festival festival = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festival, "id", 1L);
                        return festival;
                    });

            // when
            Festival festival = festivalApplicationService.create(command, principal);

            // then
            assertThat(festival.getNameValue()).isEqualTo(command.name());
            assertThat(festival.getSeriesId()).isEqualTo(10L);
            assertThat(festival.getYear()).isEqualTo(2026);
            assertThat(festival.getStartDate()).isEqualTo(command.startDate());
            assertThat(adminAccount.getFestivalId()).isEqualTo(festival.getId());
            assertThat(adminAccount.getRole()).isEqualTo(AdminRole.FESTIVAL_OWNER);

            ArgumentCaptor<Festival> captor =
                    ArgumentCaptor.forClass(Festival.class);
            then(festivalService).should().save(captor.capture());
            assertThat(captor.getValue().getAddressValue())
                    .isEqualTo(command.address());
            assertThat(captor.getValue().getDetailAddressValue())
                    .isEqualTo(command.detailAddress());
        }

        @Test
        @DisplayName("기존 축제 묶음 ID를 지정하면 해당 묶음에 축제를 생성한다")
        void success_Create_WithExistingSeries() {
            // given
            FestivalSeries festivalSeries = festivalSeries(10L);
            CreateFestivalCommand command = createCommand(festivalSeries.getPublicId());
            AdminAccount adminAccount = unassignedAdmin();
            AdminPrincipal principal = principal(null, null);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(festivalSeriesService.getByPublicId(festivalSeries.getPublicId()))
                    .willReturn(festivalSeries);
            given(festivalService.existsBySeriesIdAndYear(10L, 2026))
                    .willReturn(false);
            given(festivalService.save(any(Festival.class)))
                    .willAnswer(invocation -> {
                        Festival festival = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festival, "id", 1L);
                        return festival;
                    });

            // when
            Festival festival = festivalApplicationService.create(command, principal);

            // then
            assertThat(festival.getSeriesId()).isEqualTo(10L);
            assertThat(festival.getSeriesPublicId()).isEqualTo(festivalSeries.getPublicId());
            then(festivalSeriesService).should()
                    .getByPublicId(festivalSeries.getPublicId());
        }

        @Test
        @DisplayName("같은 축제 묶음에 같은 연도 축제가 있으면 생성할 수 없다")
        void fail_Create_DuplicatedYear_CustomException() {
            // given
            FestivalSeries festivalSeries = festivalSeries(10L);
            CreateFestivalCommand command = createCommand(festivalSeries.getPublicId());
            AdminPrincipal principal = principal(null, null);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.getByPublicId(festivalSeries.getPublicId()))
                    .willReturn(festivalSeries);
            given(festivalService.existsBySeriesIdAndYear(10L, 2026))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.create(
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_YEAR_ALREADY_EXISTS.getMessage());
        }

        @Test
        @DisplayName("지정한 축제 묶음이 없으면 생성할 수 없다")
        void fail_Create_SeriesNotFound_CustomException() {
            // given
            UUID seriesId = UUID.randomUUID();
            CreateFestivalCommand command = createCommand(seriesId);
            AdminPrincipal principal = principal(null, null);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.getByPublicId(seriesId))
                    .willThrow(new CustomException(ErrorCode.FESTIVAL_SERIES_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.create(
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_SERIES_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("선택한 축제 묶음과 축제명이 다르면 생성할 수 없다")
        void fail_Create_SeriesNameMismatch_CustomException() {
            // given
            FestivalSeries festivalSeries = festivalSeries(10L);
            CreateFestivalCommand command = new CreateFestivalCommand(
                    festivalSeries.getPublicId(),
                    "다른 축제",
                    "마포구 대표 지역 축제",
                    "서울특별시 마포구 월드컵로 243",
                    "월드컵공원",
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            AdminPrincipal principal = principal(null, null);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.getByPublicId(festivalSeries.getPublicId()))
                    .willReturn(festivalSeries);

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.create(
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("1관리자는 담당 축제 기본 정보를 수정한다")
        void success_Update_FestivalOwner() {
            // given
            Long festivalId = 1L;
            Festival festival = festival(festivalId);
            UUID publicId = festival.getPublicId();
            UpdateFestivalCommand command = updateCommand();
            AdminPrincipal principal = principal(
                    festivalId,
                    AdminRole.FESTIVAL_OWNER
            );
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(festivalOwner(festivalId));
            given(festivalService.getByPublicId(publicId))
                    .willReturn(festival);

            // when
            Festival updated = festivalApplicationService.update(
                    publicId,
                    command,
                    principal
            );

            // then
            assertThat(updated.getNameValue()).isEqualTo(command.name());
            assertThat(updated.getAddressValue()).isEqualTo(command.address());
            assertThat(updated.getDetailAddressValue())
                    .isEqualTo(command.detailAddress());
            then(festivalService).should().getByPublicId(publicId);
        }

        @Test
        @DisplayName("서브관리자는 축제 기본 정보를 수정할 수 없다")
        void fail_Update_SubAdmin_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();
            UpdateFestivalCommand command = updateCommand();
            AdminPrincipal principal = principal(1L, AdminRole.SUB_ADMIN);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(subAdmin(1L));
            given(festivalService.getByPublicId(festivalId))
                    .willReturn(festival(1L));

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.update(
                    festivalId,
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("1관리자는 다른 축제 기본 정보를 수정할 수 없다")
        void fail_Update_DifferentFestival_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();
            UpdateFestivalCommand command = updateCommand();
            AdminPrincipal principal = principal(2L, AdminRole.FESTIVAL_OWNER);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(festivalOwner(2L));
            given(festivalService.getByPublicId(festivalId))
                    .willReturn(festival(1L));

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.update(
                    festivalId,
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("축제가 없으면 수정할 수 없다")
        void fail_Update_FestivalNotFound_CustomException() {
            // given
            Long festivalId = 1L;
            UUID publicId = UUID.randomUUID();
            UpdateFestivalCommand command = updateCommand();
            AdminPrincipal principal = principal(
                    festivalId,
                    AdminRole.FESTIVAL_OWNER
            );
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(festivalOwner(festivalId));
            given(festivalService.getByPublicId(publicId))
                    .willThrow(new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.update(
                    publicId,
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("축제 개최 연도가 바뀌는 기본 정보 수정은 할 수 없다")
        void fail_Update_YearChanged_CustomException() {
            // given
            Long festivalId = 1L;
            Festival festival = festival(festivalId);
            UUID publicId = festival.getPublicId();
            UpdateFestivalCommand command = new UpdateFestivalCommand(
                    "수정 축제",
                    "수정 설명",
                    "서울특별시 마포구 수정로 1",
                    "수정 행사장",
                    LocalDate.of(2027, 11, 1),
                    LocalDate.of(2027, 11, 3),
                    LocalTime.of(9, 0),
                    LocalTime.of(20, 0)
            );
            AdminPrincipal principal = principal(
                    festivalId,
                    AdminRole.FESTIVAL_OWNER
            );
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(festivalOwner(festivalId));
            given(festivalService.getByPublicId(publicId))
                    .willReturn(festival);

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.update(
                    publicId,
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_YEAR_CANNOT_BE_CHANGED.getMessage());
        }
    }

    private CreateFestivalCommand createCommand() {
        return createCommand(null);
    }

    private CreateFestivalCommand createCommand(UUID seriesId) {
        return new CreateFestivalCommand(
                seriesId,
                "마포나루 새우젓축제",
                "마포구 대표 지역 축제",
                "서울특별시 마포구 월드컵로 243",
                "월드컵공원",
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }

    private UpdateFestivalCommand updateCommand() {
        return new UpdateFestivalCommand(
                "수정 축제",
                "수정 설명",
                "서울특별시 마포구 수정로 1",
                "수정 행사장",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3),
                LocalTime.of(9, 0),
                LocalTime.of(20, 0)
        );
    }

    private AdminPrincipal principal(
            Long festivalId,
            AdminRole role
    ) {
        return new AdminPrincipal(
                1L,
                festivalId,
                "owner@mapo.go.kr",
                role
        );
    }

    private Festival festival() {
        return festival(null);
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
        if (festivalId != null) {
            ReflectionTestUtils.setField(festival, "id", festivalId);
        }
        return festival;
    }

    private FestivalSeries festivalSeries(Long seriesId) {
        FestivalSeries festivalSeries = FestivalSeries.create(
                FestivalName.of("마포나루 새우젓축제")
        );
        ReflectionTestUtils.setField(festivalSeries, "id", seriesId);
        return festivalSeries;
    }

    private AdminAccount unassignedAdmin() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount festivalOwner(Long festivalId) {
        AdminAccount adminAccount = unassignedAdmin();
        adminAccount.assignFestivalOwner(festivalId);
        return adminAccount;
    }

    private AdminAccount subAdmin(Long festivalId) {
        return AdminAccount.createSubAdmin(
                AdminEmail.of("sub@mapo.go.kr"),
                AdminName.of("김서브"),
                AdminOrganization.of("마포구청 소속"),
                festivalId,
                AdminPasswordHash.of("encoded-password"),
                1L
        );
    }
}
