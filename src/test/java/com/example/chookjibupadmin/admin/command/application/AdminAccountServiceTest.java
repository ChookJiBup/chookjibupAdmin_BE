package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminAccountRepository;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @InjectMocks
    private AdminAccountService adminAccountService;

    @Mock
    private AdminAccountRepository adminAccountRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("관리자 계정을 저장한다")
        void success_Save() {
            // given
            AdminAccount adminAccount = adminAccount();
            given(adminAccountRepository.save(adminAccount)).willReturn(adminAccount);

            // when
            AdminAccount saved = adminAccountService.save(adminAccount);

            // then
            assertThat(saved).isSameAs(adminAccount);
            then(adminAccountRepository).should().save(adminAccount);
        }
    }

    @Nested
    @DisplayName("getAllSubAdminsByPublicIds")
    class GetAllSubAdminsByPublicIds {

        @Test
        @DisplayName("외부 UUID 목록에 해당하는 관리자 계정을 모두 조회한다")
        void success_GetAllSubAdminsByPublicIds() {
            // given
            AdminAccount first = adminAccount("first@mapo.go.kr");
            AdminAccount second = adminAccount("second@mapo.go.kr");
            List<UUID> publicIds = List.of(first.getPublicId(), second.getPublicId());
            given(adminAccountRepository.findAllByPublicIdIn(publicIds))
                    .willReturn(List.of(first, second));

            // when
            List<AdminAccount> found =
                    adminAccountService.getAllSubAdminsByPublicIds(publicIds);

            // then
            assertThat(found).containsExactly(first, second);
        }

        @Test
        @DisplayName("요청한 관리자 계정이 하나라도 없으면 예외를 던진다")
        void fail_GetAllSubAdminsByPublicIds_CustomException() {
            // given
            AdminAccount account = adminAccount("first@mapo.go.kr");
            List<UUID> publicIds = List.of(account.getPublicId(), UUID.randomUUID());
            given(adminAccountRepository.findAllByPublicIdIn(publicIds))
                    .willReturn(List.of(account));

            // when & then
            assertThatThrownBy(() ->
                    adminAccountService.getAllSubAdminsByPublicIds(publicIds))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("내부 식별자로 관리자 계정을 조회한다")
        void success_GetById() {
            // given
            AdminAccount adminAccount = adminAccount();
            given(adminAccountRepository.findById(1L))
                    .willReturn(Optional.of(adminAccount));

            // when
            AdminAccount found = adminAccountService.getById(1L);

            // then
            assertThat(found).isSameAs(adminAccount);
        }

        @Test
        @DisplayName("관리자 계정이 없으면 인증 예외를 던진다")
        void fail_GetById_CustomException() {
            // given
            given(adminAccountRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminAccountService.getById(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }
    }

    @Nested
    @DisplayName("getByEmailForLogin")
    class GetByEmailForLogin {

        @Test
        @DisplayName("로그인 이메일로 관리자 계정을 조회한다")
        void success_GetByEmailForLogin() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            AdminAccount adminAccount = adminAccount();
            given(adminAccountRepository.findByEmail(email))
                    .willReturn(Optional.of(adminAccount));

            // when
            AdminAccount found = adminAccountService.getByEmailForLogin(email);

            // then
            assertThat(found).isSameAs(adminAccount);
        }

        @Test
        @DisplayName("로그인 이메일 계정이 없으면 인증 실패 예외를 던진다")
        void fail_GetByEmailForLogin_CustomException() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            given(adminAccountRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminAccountService.getByEmailForLogin(email))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
        }
    }

    private AdminAccount adminAccount() {
        return adminAccount("admin@mapo.go.kr");
    }

    private AdminAccount adminAccount(String email) {
        return AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
