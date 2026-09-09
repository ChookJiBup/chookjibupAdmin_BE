package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.dto.ChangeFieldStaffPasswordCommand;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffPasswordChangeResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FieldStaffPasswordChangeServiceTest {

    @InjectMocks
    private FieldStaffPasswordChangeService service;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FieldStaffTokenProvider tokenProvider;

    @Nested
    @DisplayName("changeOwnPassword")
    class ChangeOwnPassword {

        @Test
        @DisplayName("현재 비밀번호가 맞으면 새 비밀번호로 바꾸고 토큰을 다시 발급한다")
        void success_ChangeOwnPassword() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            given(fieldStaffAccountService.getById(1L)).willReturn(account);
            given(passwordEncoder.matches("temp-password", "encoded-password"))
                    .willReturn(true);
            given(passwordEncoder.matches("NewPassword!123", "encoded-password"))
                    .willReturn(false);
            given(passwordEncoder.encode("NewPassword!123")).willReturn("encoded-new");
            given(tokenProvider.createAccessToken(account)).willReturn("token");
            given(tokenProvider.getAccessTokenExpirationSeconds()).willReturn(1800L);

            // when
            FieldStaffPasswordChangeResult result = service.changeOwnPassword(
                    principal(),
                    new ChangeFieldStaffPasswordCommand("temp-password", "NewPassword!123")
            );

            // then
            assertThat(account.getPasswordHashValue()).isEqualTo("encoded-new");
            assertThat(account.isPasswordChangeRequired()).isFalse();
            assertThat(account.getAuthVersion()).isEqualTo(1L);
            assertThat(result.accessToken()).isEqualTo("token");
            assertThat(result.expiresIn()).isEqualTo(1800L);
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 인증 실패로 거절한다")
        void fail_ChangeOwnPassword_InvalidCurrentPassword_CustomException() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            given(fieldStaffAccountService.getById(1L)).willReturn(account);
            given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> service.changeOwnPassword(
                    principal(),
                    new ChangeFieldStaffPasswordCommand("wrong", "NewPassword!123")
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting(exception -> ((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.FIELD_STAFF_INVALID_CREDENTIALS);
            assertThat(account.getPasswordHashValue()).isEqualTo("encoded-password");
            assertThat(account.isPasswordChangeRequired()).isTrue();
        }

        @Test
        @DisplayName("현재 비밀번호와 같은 새 비밀번호는 거절한다")
        void fail_ChangeOwnPassword_SameAsCurrent_CustomException() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            given(fieldStaffAccountService.getById(1L)).willReturn(account);
            given(passwordEncoder.matches("temp-password", "encoded-password"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> service.changeOwnPassword(
                    principal(),
                    new ChangeFieldStaffPasswordCommand("temp-password", "temp-password")
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.");
            assertThat(account.isPasswordChangeRequired()).isTrue();
        }

        @Test
        @DisplayName("인증 정보가 없으면 거절한다")
        void fail_ChangeOwnPassword_WithoutPrincipal_CustomException() {
            assertThatThrownBy(() -> service.changeOwnPassword(
                    null,
                    new ChangeFieldStaffPasswordCommand("temp-password", "NewPassword!123")
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }
    }

    private FieldStaffPrincipal principal() {
        return new FieldStaffPrincipal(1L, 10L, "staff01", 0L);
    }

    private FieldStaffAccount fieldStaffAccount() {
        FieldStaffAccount account = FieldStaffAccount.create(
                10L,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDate.of(2026, 10, 9).atStartOfDay(),
                LocalDate.of(2026, 10, 18).atTime(LocalTime.MAX)
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }
}
