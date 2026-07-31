package com.example.chookjibupadmin.operator.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccountRepository;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 스태프 계정 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FieldStaffAccountService {

    private final FieldStaffAccountRepository fieldStaffAccountRepository;

    /**
     * 현장 스태프 계정을 저장한다.
     */
    @Transactional
    public FieldStaffAccount save(FieldStaffAccount fieldStaffAccount) {
        return fieldStaffAccountRepository.save(fieldStaffAccount);
    }

    /**
     * 내부 식별자로 현장 스태프 계정을 조회한다.
     */
    public FieldStaffAccount getById(Long fieldStaffAccountId) {
        return fieldStaffAccountRepository.findById(fieldStaffAccountId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND));
    }

    /**
     * 외부 UUID로 현장 스태프 계정을 조회한다.
     */
    public FieldStaffAccount getByPublicId(UUID publicId) {
        return fieldStaffAccountRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND));
    }

    /**
     * 축제와 로그인 아이디로 현장 스태프 계정을 조회한다.
     */
    public FieldStaffAccount getByFestivalIdAndLoginIdForLogin(
            Long festivalId,
            FieldStaffLoginId loginId
    ) {
        return fieldStaffAccountRepository.findByFestivalIdAndLoginId(
                        festivalId,
                        loginId
                )
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FIELD_STAFF_INVALID_CREDENTIALS
                ));
    }

    /**
     * 축제 안에서 같은 로그인 아이디가 이미 사용 중인지 확인한다.
     */
    public boolean existsByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    ) {
        return fieldStaffAccountRepository.existsByFestivalIdAndLoginId(
                festivalId,
                loginId
        );
    }
}
