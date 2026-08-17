package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalDetail;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminManagedFestivalDetailQueryApplicationServiceTest {

    private static final UUID FESTIVAL_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SERIES_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminManagedFestivalQueryService managedFestivalQueryService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalLocationService festivalLocationService;

    private AdminManagedFestivalDetailQueryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AdminManagedFestivalDetailQueryApplicationService(
                adminAccountService,
                managedFestivalQueryService,
                festivalService,
                festivalLocationService,
                Clock.fixed(
                        Instant.parse("2026-08-08T00:00:00Z"),
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    @DisplayName("담당 축제 단건 조회는 수정 화면 필드와 전체 장소를 반환한다")
    void success_GetManagedFestival() {
        // given
        AdminAccount adminAccount = adminAccount();
        Festival festival = festival();
        FestivalLocation location = org.mockito.Mockito.mock(FestivalLocation.class);
        given(adminAccountService.getById(1L)).willReturn(adminAccount);
        given(managedFestivalQueryService.getCurrentManagedFestival(
                1L,
                FESTIVAL_ID,
                LocalDate.of(2026, 8, 8)
        )).willReturn(managedFestivalView());
        given(festivalService.getByPublicId(FESTIVAL_ID)).willReturn(festival);
        given(festivalLocationService.findAllByFestivalId(10L))
                .willReturn(List.of(location));

        // when
        AdminManagedFestivalDetail result = service.getManagedFestival(
                FESTIVAL_ID,
                new AdminPrincipal(1L, "owner@mapo.go.kr")
        );

        // then
        assertThat(result.festivalId()).isEqualTo(FESTIVAL_ID);
        assertThat(result.seriesId()).isEqualTo(SERIES_ID);
        assertThat(result.description()).isEqualTo("축제 설명");
        assertThat(result.operationStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(result.operationEndTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(result.locations()).hasSize(1);
    }

    @Test
    @DisplayName("인증 주체가 없으면 축제 상세를 조회할 수 없다")
    void fail_GetManagedFestival_Unauthorized_CustomException() {
        assertThatThrownBy(() -> service.getManagedFestival(FESTIVAL_ID, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    private AdminAccount adminAccount() {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("password-hash")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
    }

    private Festival festival() {
        Festival festival = Festival.create(
                FESTIVAL_ID,
                20L,
                SERIES_ID,
                FestivalName.of("테스트 축제"),
                FestivalDescription.of("축제 설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalDetailAddress.of(null),
                FestivalPeriod.of(
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 12)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
        ReflectionTestUtils.setField(festival, "id", 10L);
        return festival;
    }

    private AdminManagedFestivalView managedFestivalView() {
        return new AdminManagedFestivalView(
                FESTIVAL_ID,
                "테스트 축제",
                2026,
                AdminRole.FESTIVAL_OWNER,
                FestivalStatus.DRAFT,
                FestivalProgressStatus.UPCOMING,
                "서울특별시 마포구",
                null,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12)
        );
    }
}
