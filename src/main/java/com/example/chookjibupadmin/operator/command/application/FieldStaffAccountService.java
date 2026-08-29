package com.example.chookjibupadmin.operator.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccountRepository;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * 내부 식별자로 현장 스태프 계정을 선택적으로 조회한다.
     */
    public Optional<FieldStaffAccount> findById(Long fieldStaffAccountId) {
        return fieldStaffAccountRepository.findById(fieldStaffAccountId);
    }

    /**
     * 내부 식별자 목록에 해당하는 현장 스태프 계정을 조회한다.
     */
    public List<FieldStaffAccount> findAllById(Collection<Long> fieldStaffAccountIds) {
        return fieldStaffAccountRepository.findAllById(fieldStaffAccountIds);
    }

    /**
     * 내부 식별자로 현장 스태프 계정을 조회한다.
     */
    public FieldStaffAccount getById(Long fieldStaffAccountId) {
        return fieldStaffAccountRepository.findById(fieldStaffAccountId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND));
    }

    /**
     * 토큰 Claim과 현재 계정 상태가 모두 유효한지 검증한다.
     */
    public void validateAuthentication(
            FieldStaffPrincipal principal,
            LocalDateTime now
    ) {
        if (principal == null || now == null) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        FieldStaffAccount account = fieldStaffAccountRepository
                .findById(principal.fieldStaffId())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_TOKEN_INVALID));

        if (!account.getFestivalId().equals(principal.festivalId())
                || !account.getLoginIdValue().equals(principal.loginId())
                || account.getAuthVersion() != principal.authVersion()) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        if (!account.isActive()) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_ACTIVE);
        }
        if (!account.isWithinValidPeriod(now)) {
            throw new CustomException(ErrorCode.FIELD_STAFF_VALID_PERIOD_EXPIRED);
        }
    }

    /**
     * 외부 UUID로 현장 스태프 계정을 조회한다.
     */
    public FieldStaffAccount getByPublicId(UUID publicId) {
        return fieldStaffAccountRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND));
    }

    /**
     * 외부 UUID 목록에 해당하는 모든 현장 스태프 계정을 조회한다.
     */
    public List<FieldStaffAccount> getAllByPublicIds(
            Collection<UUID> publicIds
    ) {
        List<FieldStaffAccount> accounts =
                fieldStaffAccountRepository.findAllByPublicIdIn(publicIds);
        if (accounts.size() != publicIds.size()) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND);
        }
        return accounts;
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

    /**
     * 축제에 속한 현장 스태프 계정을 모두 삭제한다.
     */
    @Transactional
    public void deleteAllByFestivalId(Long festivalId) {
        fieldStaffAccountRepository.deleteAllByFestivalId(festivalId);
    }
}
