package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
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
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalOperationAccessServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 10, 10, 9, 0);

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    private FestivalOperationAccessService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-10T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new FestivalOperationAccessService(
                festivalService,
                adminAccountService,
                adminFestivalRoleService,
                fieldStaffAccountService,
                clock
        );
    }

    @Test
    void success_GetAuthorizedFestivalId_ManagedAdmin() {
        // given
        Festival festival = festival(10L);
        AdminPrincipal principal = new AdminPrincipal(
                1L,
                "admin@mapo.go.kr",
                3L
        );
        given(festivalService.getByPublicId(festival.getPublicId()))
                .willReturn(festival);
        given(adminAccountService.isAuthenticationValid(1L, 3L))
                .willReturn(true);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                principal.adminId(),
                festival.getId()
        )).willReturn(AdminFestivalRole.createSubAdmin(
                principal.adminId(),
                festival.getId(),
                2L
        ));

        // when
        Long result = service.getAuthorizedFestivalId(
                festival.getPublicId(),
                principal
        );

        // then
        assertThat(result).isEqualTo(festival.getId());
    }

    @Test
    void fail_GetAuthorizedFestivalId_StaleAdminToken_CustomException() {
        // given
        Festival festival = festival(10L);
        AdminPrincipal principal = new AdminPrincipal(
                1L,
                "admin@mapo.go.kr",
                2L
        );
        given(festivalService.getByPublicId(festival.getPublicId()))
                .willReturn(festival);
        given(adminAccountService.isAuthenticationValid(1L, 2L))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> service.getAuthorizedFestivalId(
                festival.getPublicId(),
                principal
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        then(adminFestivalRoleService).shouldHaveNoInteractions();
    }

    @Test
    void success_GetAuthorizedFestivalId_OwnFestivalFieldStaff() {
        // given
        Festival festival = festival(10L);
        FieldStaffPrincipal principal = new FieldStaffPrincipal(
                1L,
                festival.getId(),
                "staff01",
                0L
        );
        given(festivalService.getByPublicId(festival.getPublicId()))
                .willReturn(festival);

        // when
        Long result = service.getAuthorizedFestivalId(
                festival.getPublicId(),
                principal
        );

        // then
        assertThat(result).isEqualTo(festival.getId());
        then(fieldStaffAccountService).should().validateAuthentication(
                principal,
                NOW
        );
    }

    @Test
    void fail_GetAuthorizedFestivalId_OtherFestivalFieldStaff_CustomException() {
        // given
        Festival festival = festival(10L);
        FieldStaffPrincipal principal = new FieldStaffPrincipal(
                1L,
                11L,
                "staff01",
                0L
        );
        given(festivalService.getByPublicId(festival.getPublicId()))
                .willReturn(festival);

        // when & then
        assertThatThrownBy(() -> service.getAuthorizedFestivalId(
                festival.getPublicId(),
                principal
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void fail_GetAuthorizedFestivalId_MissingPrincipal_CustomException() {
        assertThatThrownBy(() -> service.getAuthorizedFestivalId(
                UUID.randomUUID(),
                null
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
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
}
