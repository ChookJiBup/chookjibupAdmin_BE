package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 본인 계정을 탈퇴 상태로 변경하는 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminWithdrawService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;

    /**
     * 인증된 관리자 계정을 물리 삭제하지 않고 탈퇴 상태로 변경한다.
     * 총괄 관리자 역할이 남아 있으면 탈퇴할 수 없다. 운영자(제2관리자)는 탈퇴할 수 있다.
     */
    public void withdraw(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (adminAccount.isDeleted()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_ALREADY_WITHDRAWN);
        }
        if (adminFestivalRoleService.hasFestivalOwnerRole(adminAccount.getId())) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_WITHDRAW_HAS_OWNER_ROLE);
        }
        adminAccount.withdraw();
    }
}
