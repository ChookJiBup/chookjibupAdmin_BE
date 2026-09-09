package com.example.chookjibupadmin.operator.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.dto.ChangeFieldStaffPasswordCommand;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffPasswordChangeResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 스태프가 관리자에게 받은 임시 비밀번호를 본인 비밀번호로 바꾸는 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FieldStaffPasswordChangeService {

    private static final String SAME_AS_CURRENT_PASSWORD_MESSAGE =
            "새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.";

    private final FieldStaffAccountService fieldStaffAccountService;
    private final PasswordEncoder passwordEncoder;
    private final FieldStaffTokenProvider tokenProvider;

    /**
     * 현재 비밀번호를 확인한 뒤 새 비밀번호로 교체하고 Access Token을 다시 발급한다.
     *
     * <p>비밀번호를 바꾸면 인증 버전이 올라가 기존 토큰이 무효해지므로,
     * 스태프가 곧바로 로그아웃되지 않도록 새 토큰을 함께 발급한다.</p>
     */
    public FieldStaffPasswordChangeResult changeOwnPassword(
            FieldStaffPrincipal principal,
            ChangeFieldStaffPasswordCommand command
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        FieldStaffAccount account = fieldStaffAccountService
                .getById(principal.fieldStaffId());

        if (!passwordEncoder.matches(
                command.currentPassword(),
                account.getPasswordHashValue()
        )) {
            throw new CustomException(ErrorCode.FIELD_STAFF_INVALID_CREDENTIALS);
        }
        if (passwordEncoder.matches(
                command.newPassword(),
                account.getPasswordHashValue()
        )) {
            throw new CustomException(
                    ErrorCode.INVALID_REQUEST,
                    SAME_AS_CURRENT_PASSWORD_MESSAGE
            );
        }

        account.changePasswordBySelf(FieldStaffPasswordHash.of(
                passwordEncoder.encode(command.newPassword())
        ));

        return new FieldStaffPasswordChangeResult(
                tokenProvider.createAccessToken(account),
                tokenProvider.getAccessTokenExpirationSeconds()
        );
    }
}
