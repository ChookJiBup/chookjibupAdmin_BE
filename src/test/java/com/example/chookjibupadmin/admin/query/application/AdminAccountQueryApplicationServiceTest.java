package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminAccountProfileView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminAccountQueryApplicationServiceTest {

    @Mock
    private AdminAccountService adminAccountService;

    @InjectMocks
    private AdminAccountQueryApplicationService service;

    @Test
    void success_GetMyProfile_ActiveAccount() {
        // given
        AdminAccount account = adminAccount();
        given(adminAccountService.getById(1L)).willReturn(account);

        // when
        AdminAccountProfileView result = service.getMyProfile(
                new AdminPrincipal(1L, account.getEmailValue())
        );

        // then
        assertThat(result.adminId()).isEqualTo(account.getPublicId());
        assertThat(result.email()).isEqualTo("admin@mapo.go.kr");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.organization()).isEqualTo("마포구청");
        assertThat(result.department()).isEqualTo("문화예술과");
        assertThat(result.rank()).isEqualTo("주무관");
        assertThat(result.status()).isEqualTo(AdminStatus.ACTIVE);
    }

    @Test
    void fail_GetMyProfile_MissingPrincipal_CustomException() {
        assertThatThrownBy(() -> service.getMyProfile(null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void fail_GetMyProfile_InactiveAccount_CustomException() {
        // given
        AdminAccount account = adminAccount();
        account.withdraw();
        given(adminAccountService.getById(1L)).willReturn(account);

        // when & then
        assertThatThrownBy(() -> service.getMyProfile(
                new AdminPrincipal(1L, account.getEmailValue())
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
    }

    private AdminAccount adminAccount() {
        AdminAccount account = AdminAccount.createAdmin(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청"),
                AdminDepartment.of("문화예술과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("password-hash")
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }
}
