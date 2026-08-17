package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.query.application.dto.AdminAccountProfileView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 본인 정보 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountQueryApplicationService {

    private final AdminAccountService adminAccountService;

    /**
     * 로그인한 관리자의 계정 정보를 조회한다.
     */
    public AdminAccountProfileView getMyProfile(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }

        return new AdminAccountProfileView(
                adminAccount.getPublicId(),
                adminAccount.getEmailValue(),
                adminAccount.getNameValue(),
                adminAccount.getOrganizationValue(),
                adminAccount.getRankValue(),
                adminAccount.getStatus()
        );
    }
}
