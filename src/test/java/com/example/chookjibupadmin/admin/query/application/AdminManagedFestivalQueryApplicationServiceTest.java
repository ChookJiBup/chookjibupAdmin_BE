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
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminManagedFestivalQueryApplicationServiceTest {

    private AdminManagedFestivalQueryApplicationService applicationService;

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 1);

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminManagedFestivalQueryService managedFestivalQueryService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-31T15:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        applicationService = new AdminManagedFestivalQueryApplicationService(
                adminAccountService,
                managedFestivalQueryService,
                clock
        );
    }

    @Nested
    @DisplayName("searchManagedFestivals")
    class SearchManagedFestivals {

        @Test
        @DisplayName("인증 관리자의 현재 관리 축제 목록을 조회한다")
        void success_SearchManagedFestivals() {
            // given
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            AdminManagedFestivalCondition condition =
                    new AdminManagedFestivalCondition(null, null, null);
            AdminManagedFestivalView view = managedFestivalView();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(managedFestivalQueryService.searchCurrentManagedFestivals(
                    adminAccount.getId(),
                    condition,
                    TODAY
            )).willReturn(List.of(view));

            // when
            List<AdminManagedFestivalView> result =
                    applicationService.searchManagedFestivals(
                            condition,
                            principal
                    );

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("비활성 관리자 계정은 관리 축제 목록을 조회할 수 없다")
        void fail_SearchManagedFestivals_InactiveAdmin_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            adminAccount.withdraw();
            AdminPrincipal principal = principal(adminAccount);
            AdminManagedFestivalCondition condition =
                    new AdminManagedFestivalCondition(null, null, null);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);

            // when & then
            assertThatThrownBy(() -> applicationService.searchManagedFestivals(
                    condition,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
        }

        @Test
        @DisplayName("인증 주체가 없으면 인증 예외를 던진다")
        void fail_SearchManagedFestivals_CustomException() {
            // given
            AdminManagedFestivalCondition condition =
                    new AdminManagedFestivalCondition(null, null, null);
            AdminPrincipal principal = null;

            // when & then
            assertThatThrownBy(() ->
                    applicationService.searchManagedFestivals(
                            condition,
                            principal
                    )
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }
    }

    @Nested
    @DisplayName("getManagedFestival")
    class GetManagedFestival {

        @Test
        @DisplayName("인증 관리자의 현재 관리 축제를 단건 조회한다")
        void success_GetManagedFestival() {
            // given
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            UUID festivalId = UUID.randomUUID();
            AdminManagedFestivalView view = managedFestivalView(festivalId);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(managedFestivalQueryService.getCurrentManagedFestival(
                    adminAccount.getId(),
                    festivalId,
                    TODAY
            )).willReturn(view);

            // when
            AdminManagedFestivalView result =
                    applicationService.getManagedFestival(
                            festivalId,
                            principal
                    );

            // then
            assertThat(result).isEqualTo(view);
        }
    }

    private AdminAccount adminAccount() {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private AdminManagedFestivalView managedFestivalView() {
        return managedFestivalView(UUID.randomUUID());
    }

    private AdminManagedFestivalView managedFestivalView(UUID festivalId) {
        return new AdminManagedFestivalView(
                festivalId,
                "마포나루 새우젓축제",
                2026,
                AdminRole.FESTIVAL_OWNER,
                FestivalStatus.DRAFT,
                FestivalProgressStatus.UPCOMING,
                "서울특별시 마포구 월드컵로 243",
                "월드컵공원",
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18)
        );
    }
}
