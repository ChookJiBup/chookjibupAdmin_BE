package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminAccountRepository;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 계정 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountService {

    private final AdminAccountRepository adminAccountRepository;

    /**
     * 관리자 계정을 저장한다.
     */
    @Transactional
    public AdminAccount save(AdminAccount adminAccount) {
        return adminAccountRepository.save(adminAccount);
    }

    /**
     * 내부 식별자로 관리자 계정을 조회한다.
     */
    public AdminAccount getById(Long adminAccountId) {
        return adminAccountRepository.findById(adminAccountId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 외부 UUID로 관리자 계정을 조회한다.
     */
    public AdminAccount getByPublicId(UUID publicId) {
        return adminAccountRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 제2관리자 삭제 대상 UUID에 해당하는 관리자 계정을 모두 조회한다.
     */
    public List<AdminAccount> getAllSubAdminsByPublicIds(
            Collection<UUID> publicIds
    ) {
        List<AdminAccount> accounts =
                adminAccountRepository.findAllByPublicIdIn(publicIds);
        if (accounts.size() != publicIds.size()) {
            throw new CustomException(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND);
        }
        return accounts;
    }

    /**
     * 로그인 이메일로 관리자 계정을 조회한다.
     */
    public AdminAccount getByEmailForLogin(AdminEmail email) {
        return adminAccountRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));
    }

    /**
     * 같은 이메일로 가입된 계정이 있는지 확인한다.
     */
    public boolean existsByEmail(AdminEmail email) {
        return adminAccountRepository.existsByEmail(email);
    }

}
