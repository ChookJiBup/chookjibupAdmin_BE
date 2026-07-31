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

    /**
     * 인증된 관리자 계정을 물리 삭제하지 않고 탈퇴 상태로 변경한다.
     */
    public void withdraw(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        adminAccount.withdraw();
    }
}
