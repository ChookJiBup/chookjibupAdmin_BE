package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminAccountRepository;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * 내부 식별자로 관리자 계정을 선택적으로 조회한다.
     */
    public Optional<AdminAccount> findById(Long adminAccountId) {
        return adminAccountRepository.findById(adminAccountId);
    }

    /**
     * 내부 식별자 목록에 해당하는 관리자 계정을 조회한다.
     */
    public List<AdminAccount> findAllById(Collection<Long> adminAccountIds) {
        return adminAccountRepository.findAllById(adminAccountIds);
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
     * 외부 UUID에 해당하는 관리자 계정을 선택적으로 조회한다.
     */
    public Optional<AdminAccount> findByPublicId(UUID publicId) {
        return adminAccountRepository.findByPublicId(publicId);
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
     * 이메일에 해당하는 관리자 계정을 선택적으로 조회한다.
     */
    public Optional<AdminAccount> findByEmail(AdminEmail email) {
        return adminAccountRepository.findByEmail(email);
    }

    /**
     * 비밀번호 재설정 결과를 저장한다.
     */
    @Transactional
    public void changePassword(
            AdminAccount adminAccount,
            AdminPasswordHash passwordHash
    ) {
        adminAccount.changePassword(passwordHash);
        adminAccountRepository.save(adminAccount);
    }

    /** 관리자 본인의 이름, 소속, 직급 변경 결과를 저장한다. */
    @Transactional
    public void updateProfile(
            Long adminAccountId,
            AdminName name,
            AdminOrganization organization,
            AdminRank rank
    ) {
        AdminAccount adminAccount = getById(adminAccountId);
        if (adminAccount.isContractor()) {
            adminAccount.updateContractorProfile(name, organization);
        } else {
            if (rank == null) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            adminAccount.updateGovernmentProfile(name, organization, rank);
        }
        adminAccountRepository.save(adminAccount);
    }

    /**
     * JWT에 기록된 인증 버전이 현재 계정과 같은지 확인한다.
     */
    public boolean isAuthenticationValid(Long adminAccountId, long authVersion) {
        return adminAccountRepository.findById(adminAccountId)
                .filter(AdminAccount::isActive)
                .map(account -> account.getAuthVersion() == authVersion)
                .orElse(false);
    }

    /**
     * 같은 이메일로 가입된 계정이 있는지 확인한다.
     */
    public boolean existsByEmail(AdminEmail email) {
        return adminAccountRepository.existsByEmail(email);
    }

}
