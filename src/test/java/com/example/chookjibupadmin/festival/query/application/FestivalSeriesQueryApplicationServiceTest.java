package com.example.chookjibupadmin.festival.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalSeriesQueryApplicationServiceTest {

    @InjectMocks
    private FestivalSeriesQueryApplicationService applicationService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private FestivalSeriesQueryService queryService;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("인증 관리자는 기본 조회 수로 기존 축제를 검색한다")
        void success_Search_DefaultLimit() {
            // given
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            FestivalSeriesSearchView view = view();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(queryService.search("김밥", 10))
                    .willReturn(List.of(view));

            // when
            List<FestivalSeriesSearchView> result =
                    applicationService.search("김밥", null, principal);

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("인증 주체가 없으면 인증 예외를 던진다")
        void fail_Search_Unauthorized_CustomException() {
            // given
            AdminPrincipal principal = null;

            // when & then
            assertThatThrownBy(() ->
                    applicationService.search("김밥", 10, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 관리자는 기존 축제를 검색할 수 없다")
        void fail_Search_InactiveAdmin_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            adminAccount.withdraw();
            AdminPrincipal principal = principal(adminAccount);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);

            // when & then
            assertThatThrownBy(() ->
                    applicationService.search("김밥", 10, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
        }
    }

    private AdminAccount adminAccount() {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
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

    private FestivalSeriesSearchView view() {
        return new FestivalSeriesSearchView(
                null, "김밥축제", null, null, null, null, null,
                null, null, null, null
        );
    }
}
