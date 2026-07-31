package com.example.chookjibupadmin.operator.command.application;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginCommand;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 스태프 아이디/비밀번호 로그인과 JWT 발급을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class FieldStaffLoginService {

    private final FieldStaffAccountService fieldStaffAccountService;
    private final FestivalService festivalService;
    private final PasswordEncoder passwordEncoder;
    private final FieldStaffTokenProvider tokenProvider;
    private final Clock clock;

    /**
     * 축제별 현장 스태프 계정 인증에 성공하면 Access Token을 반환한다.
     */
    @Transactional(readOnly = true)
    public FieldStaffLoginResult login(FieldStaffLoginCommand command) {
        Festival festival = festivalService.getByPublicId(command.festivalId());
        FieldStaffAccount fieldStaffAccount = fieldStaffAccountService
                .getByFestivalIdAndLoginIdForLogin(
                        festival.getId(),
                        FieldStaffLoginId.of(command.loginId())
                );

        if (!passwordEncoder.matches(
                command.password(),
                fieldStaffAccount.getPasswordHashValue()
        )) {
            throw new CustomException(ErrorCode.FIELD_STAFF_INVALID_CREDENTIALS);
        }

        if (fieldStaffAccount.isDeleted()) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_ACTIVE);
        }

        if (!fieldStaffAccount.isUsableAt(LocalDateTime.now(clock))) {
            throw new CustomException(ErrorCode.FIELD_STAFF_VALID_PERIOD_EXPIRED);
        }

        return new FieldStaffLoginResult(
                tokenProvider.createAccessToken(fieldStaffAccount),
                tokenProvider.getAccessTokenExpirationSeconds(),
                fieldStaffAccount,
                festival.getPublicId()
        );
    }
}
