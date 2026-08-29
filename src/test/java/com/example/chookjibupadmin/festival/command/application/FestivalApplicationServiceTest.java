package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.command.application.dto.UpdateFestivalCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalLocationService festivalLocationService;

    @Mock
    private com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService
            visitorCountService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("장소 목록에 null 항목이 있으면 500 대신 잘못된 요청으로 거절한다")
        void fail_Create_NullLocationReturnsInvalidRequest() {
            List<FestivalLocationCommand> locations =
                    new java.util.ArrayList<>(createCommand().locations());
            locations.add(null);
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    locations,
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries series = invocation.getArgument(0);
                        ReflectionTestUtils.setField(series, "id", 10L);
                        return series;
                    });

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 기본 정보를 저장하고 생성자를 1관리자로 배정한다")
        void success_Create_AssignFestivalOwner() {
            // given
            CreateFestivalCommand command = createCommand();
            AdminAccount adminAccount = unassignedAdmin();
            AdminPrincipal principal = principal();
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
            then(adminFestivalRoleService).should()
                    .assignFestivalOwner(1L, festival.getId());

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
            AdminPrincipal principal = principal();
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
            AdminPrincipal principal = principal();
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
            AdminPrincipal principal = principal();
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
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "월드컵공원",
                            "서울특별시 마포구 월드컵로 243",
                            null,
                            "월드컵공원",
                            null,
                            null,
                            new BigDecimal("37.5683000"),
                            new BigDecimal("126.8973000"),
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            AdminPrincipal principal = principal();
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

        @Test
        @DisplayName("외부업자 계정은 축제를 생성할 수 없다")
        void fail_Create_ContractorForbidden() {
            // given
            CreateFestivalCommand command = createCommand();
            AdminAccount contractor = AdminAccount.createContractor(
                    AdminEmail.of("vendor@gmail.com"),
                    AdminName.of("김업체"),
                    AdminOrganization.of("축제기획(주)"),
                    AdminPasswordHash.of("encoded-password")
            );
            ReflectionTestUtils.setField(contractor, "id", 2L);
            AdminPrincipal principal = new AdminPrincipal(2L, contractor.getEmailValue());
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(contractor);

            // when & then
            assertThatThrownBy(() -> festivalApplicationService.create(
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_FESTIVAL_CREATE_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("대표 장소 위경도가 없으면 생성할 수 없다")
        void fail_Create_PrimaryCoordinatesRequired() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "월드컵공원",
                            "서울특별시 마포구 월드컵로 243",
                            null,
                            "중앙광장",
                            null,
                            null,
                            null,
                            null,
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
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

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("한국 영역 밖 좌표면 생성할 수 없다")
        void fail_Create_CoordinatesOutOfKorea() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "월드컵공원",
                            "서울특별시 마포구 월드컵로 243",
                            null,
                            "중앙광장",
                            null,
                            null,
                            new BigDecimal("35.6812"),
                            new BigDecimal("139.7671"),
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
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

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA.getMessage());
        }

        @Test
        @DisplayName("보조 장소 좌표가 null이면 생성이 가능하다")
        void success_Create_SecondaryWithoutCoordinates() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(
                            new FestivalLocationCommand(
                                    FestivalLocationType.MAIN_VENUE,
                                    "월드컵공원",
                                    "서울특별시 마포구 월드컵로 243",
                                    null,
                                    null,
                                    null,
                                    null,
                                    new BigDecimal("37.5683"),
                                    new BigDecimal("126.8973"),
                                    true,
                                    0
                            ),
                            new FestivalLocationCommand(
                                    FestivalLocationType.PARKING,
                                    "임시 주차장",
                                    "서울특별시 마포구 성산동 123",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    1
                            )
                    ),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);
            given(festivalService.save(any(Festival.class)))
                    .willAnswer(invocation -> {
                        Festival festival = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festival, "id", 1L);
                        return festival;
                    });

            Festival festival = festivalApplicationService.create(command, principal());
            assertThat(festival.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("위도만 있으면 생성할 수 없다")
        void fail_Create_LatitudeOnly() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "월드컵공원",
                            "서울특별시 마포구 월드컵로 243",
                            null,
                            null,
                            null,
                            null,
                            new BigDecimal("37.5683"),
                            null,
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("경도만 있으면 생성할 수 없다")
        void fail_Create_LongitudeOnly() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "월드컵공원",
                            "서울특별시 마포구 월드컵로 243",
                            null,
                            null,
                            null,
                            null,
                            null,
                            new BigDecimal("126.8973"),
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("보조 장소 좌표가 한국 인근 범위 밖이면 거절한다")
        void fail_Create_SecondaryOutOfKorea() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(
                            new FestivalLocationCommand(
                                    FestivalLocationType.MAIN_VENUE,
                                    "월드컵공원",
                                    "서울특별시 마포구 월드컵로 243",
                                    null,
                                    null,
                                    null,
                                    null,
                                    new BigDecimal("37.5683"),
                                    new BigDecimal("126.8973"),
                                    true,
                                    0
                            ),
                            new FestivalLocationCommand(
                                    FestivalLocationType.PARKING,
                                    "해외 주차장",
                                    "도쿄",
                                    null,
                                    null,
                                    null,
                                    null,
                                    new BigDecimal("35.6812"),
                                    new BigDecimal("139.7671"),
                                    false,
                                    1
                            )
                    ),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA.getMessage());
        }

        @Test
        @DisplayName("한국 인근 범위 바로 밖 좌표는 거절한다")
        void fail_Create_JustOutsideKoreaBoundary() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "범위 밖 행사장",
                            "검증용 주소",
                            null,
                            null,
                            null,
                            null,
                            new BigDecimal("32.9999999"),
                            new BigDecimal("124.5"),
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA.getMessage());
        }

        @Test
        @DisplayName("보조 장소 위도만 있으면 거절한다")
        void fail_Create_SecondaryLatitudeOnly() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    List.of(
                            new FestivalLocationCommand(
                                    FestivalLocationType.MAIN_VENUE,
                                    "월드컵공원",
                                    "서울특별시 마포구 월드컵로 243",
                                    null,
                                    null,
                                    null,
                                    null,
                                    new BigDecimal("37.5683"),
                                    new BigDecimal("126.8973"),
                                    true,
                                    0
                            ),
                            new FestivalLocationCommand(
                                    FestivalLocationType.PARKING,
                                    "임시 주차장",
                                    "서울특별시 마포구 성산동 123",
                                    null,
                                    null,
                                    null,
                                    null,
                                    new BigDecimal("37.56"),
                                    null,
                                    false,
                                    1
                            )
                    ),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("한국 범위 경계값(33.0, 38.7, 124.5, 132.0)은 허용한다")
        void success_Create_KoreaBoundaryCoordinates() {
            for (BigDecimal[] point : new BigDecimal[][]{
                    {new BigDecimal("33.0"), new BigDecimal("124.5")},
                    {new BigDecimal("38.7"), new BigDecimal("132.0")}
            }) {
                CreateFestivalCommand command = new CreateFestivalCommand(
                        null,
                        "마포나루 새우젓축제",
                        "마포구 대표 지역 축제",
                        List.of(new FestivalLocationCommand(
                                FestivalLocationType.MAIN_VENUE,
                                "경계 행사장",
                                "검증용 주소",
                                null,
                                null,
                                null,
                                null,
                                point[0],
                                point[1],
                                true,
                                0
                        )),
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18),
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                );
                given(adminAccountService.getById(principal().adminId()))
                        .willReturn(unassignedAdmin());
                given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                        .willReturn(java.util.Optional.empty());
                given(festivalSeriesService.save(any(FestivalSeries.class)))
                        .willAnswer(invocation -> {
                            FestivalSeries festivalSeries = invocation.getArgument(0);
                            ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                            return festivalSeries;
                        });
                given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);
                given(festivalService.save(any(Festival.class)))
                        .willAnswer(invocation -> {
                            Festival festival = invocation.getArgument(0);
                            ReflectionTestUtils.setField(festival, "id", 1L);
                            return festival;
                        });

                assertThat(festivalApplicationService.create(command, principal()).getId())
                        .isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("레거시 주소 전용 생성은 좌표 누락으로 거절한다")
        void fail_Create_LegacyAddressWithoutCoordinates() {
            CreateFestivalCommand command = new CreateFestivalCommand(
                    null,
                    "마포나루 새우젓축제",
                    "마포구 대표 지역 축제",
                    "서울특별시 마포구 월드컵로 243",
                    "월드컵공원",
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(10, 0),
                    LocalTime.of(21, 0)
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalSeriesService.findByNormalizedName("마포나루새우젓축제"))
                    .willReturn(java.util.Optional.empty());
            given(festivalSeriesService.save(any(FestivalSeries.class)))
                    .willAnswer(invocation -> {
                        FestivalSeries festivalSeries = invocation.getArgument(0);
                        ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                        return festivalSeries;
                    });
            given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

            assertThatThrownBy(() -> festivalApplicationService.create(command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED.getMessage());
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
            AdminPrincipal principal = principal();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(publicId))
                    .willReturn(festival);
            givenOwnerRole(1L, festivalId);

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
            then(festivalService).should().getByPublicIdForUpdate(publicId);
        }

        @Test
        @DisplayName("수정 시 대표 장소 좌표가 없으면 거절한다")
        void fail_Update_PrimaryCoordinatesRequired() {
            Long festivalId = 1L;
            Festival festival = festival(festivalId);
            UUID publicId = festival.getPublicId();
            UpdateFestivalCommand command = new UpdateFestivalCommand(
                    "수정 축제",
                    "수정 설명",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "수정 행사장",
                            "서울특별시 마포구 수정로 1",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(9, 0),
                    LocalTime.of(20, 0),
                    null
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(publicId)).willReturn(festival);
            givenOwnerRole(1L, festivalId);

            assertThatThrownBy(() -> festivalApplicationService.update(publicId, command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("수정 시 대표 장소 좌표가 한국 범위 밖이면 거절한다")
        void fail_Update_CoordinatesOutOfKorea() {
            Long festivalId = 1L;
            Festival festival = festival(festivalId);
            UUID publicId = festival.getPublicId();
            UpdateFestivalCommand command = new UpdateFestivalCommand(
                    "수정 축제",
                    "수정 설명",
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "수정 행사장",
                            "서울특별시 마포구 수정로 1",
                            null,
                            null,
                            null,
                            null,
                            new BigDecimal("35.6812"),
                            new BigDecimal("139.7671"),
                            true,
                            0
                    )),
                    LocalDate.of(2026, 10, 16),
                    LocalDate.of(2026, 10, 18),
                    LocalTime.of(9, 0),
                    LocalTime.of(20, 0),
                    null
            );
            given(adminAccountService.getById(principal().adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(publicId)).willReturn(festival);
            givenOwnerRole(1L, festivalId);

            assertThatThrownBy(() -> festivalApplicationService.update(publicId, command, principal()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA.getMessage());
        }

        @Test
        @DisplayName("방문 인원 데이터가 있으면 입력 모드를 변경할 수 없다")
        void fail_Update_VisitorInputModeChangeForbidden_CustomException() {
            Long festivalId = 1L;
            Festival festival = festival(festivalId);
            festival.changeVisitorCountInputMode(FestivalVisitorCountInputMode.DAILY);
            UUID publicId = festival.getPublicId();
            AdminPrincipal principal = principal();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(publicId))
                    .willReturn(festival);
            givenOwnerRole(1L, festivalId);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(festivalId))
                    .willReturn(List.of(FestivalDailyVisitorCount.create(
                            festivalId,
                            LocalDate.of(2026, 10, 16),
                            VisitorCount.of(100)
                    )));

            assertThatThrownBy(() -> festivalApplicationService.update(
                    publicId,
                    updateCommand(FestivalVisitorCountInputMode.TOTAL),
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_VISITOR_INPUT_MODE_CHANGE_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("서브관리자는 축제 기본 정보를 수정할 수 없다")
        void fail_Update_SubAdmin_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();
            UpdateFestivalCommand command = updateCommand();
            AdminPrincipal principal = principal();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(festivalId))
                    .willReturn(festival(1L));
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 1L))
                    .willReturn(AdminFestivalRole.createSubAdmin(1L, 1L, 2L));

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
            AdminPrincipal principal = principal();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(festivalId))
                    .willReturn(festival(1L));
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 1L))
                    .willThrow(new CustomException(ErrorCode.FORBIDDEN));

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
            AdminPrincipal principal = principal();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(publicId))
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
                    List.of(new FestivalLocationCommand(
                            FestivalLocationType.MAIN_VENUE,
                            "수정 행사장",
                            "서울특별시 마포구 수정로 1",
                            null,
                            "수정 행사장",
                            null,
                            null,
                            new BigDecimal("37.5665000"),
                            new BigDecimal("126.9780000"),
                            true,
                            0
                    )),
                    LocalDate.of(2027, 11, 1),
                    LocalDate.of(2027, 11, 3),
                    LocalTime.of(9, 0),
                    LocalTime.of(20, 0),
                    null
            );
            AdminPrincipal principal = principal();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(festivalService.getByPublicIdForUpdate(publicId))
                    .willReturn(festival);
            givenOwnerRole(1L, festivalId);

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
                List.of(new FestivalLocationCommand(
                        FestivalLocationType.MAIN_VENUE,
                        "월드컵공원",
                        "서울특별시 마포구 월드컵로 243",
                        null,
                        "월드컵공원",
                        null,
                        null,
                        new BigDecimal("37.5683000"),
                        new BigDecimal("126.8973000"),
                        true,
                        0
                )),
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }

    private UpdateFestivalCommand updateCommand() {
        return updateCommand(null);
    }

    private UpdateFestivalCommand updateCommand(FestivalVisitorCountInputMode visitorCountInputMode) {
        return new UpdateFestivalCommand(
                "수정 축제",
                "수정 설명",
                List.of(new FestivalLocationCommand(
                        FestivalLocationType.MAIN_VENUE,
                        "수정 행사장",
                        "서울특별시 마포구 수정로 1",
                        null,
                        "수정 행사장",
                        null,
                        null,
                        new BigDecimal("37.5665000"),
                        new BigDecimal("126.9780000"),
                        true,
                        0
                )),
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(9, 0),
                LocalTime.of(20, 0),
                visitorCountInputMode
        );
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "owner@mapo.go.kr");
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
        AdminAccount admin = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        return admin;
    }

    private void givenOwnerRole(Long adminId, Long festivalId) {
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                adminId,
                festivalId
        )).willReturn(AdminFestivalRole.createFestivalOwner(adminId, festivalId));
    }
}
