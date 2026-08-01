package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 개인 관리 축제 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagedFestivalQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminManagedFestivalQueryService managedFestivalQueryService;
    private final Clock clock;

    /**
     * 인증 관리자가 현재 관리 중인 축제 목록을 조회한다.
     *
     * TODO(admin): 관리자 축제 참여 이력 테이블 도입 후 과거 이력까지 조회하도록 확장한다.
     */
    public List<AdminManagedFestivalView> searchManagedFestivals(
            AdminManagedFestivalCondition condition,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        return managedFestivalQueryService.searchCurrentManagedFestivals(
                adminAccount.getId(),
                condition,
                LocalDate.now(clock)
        );
    }

    /**
     * 인증 관리자가 현재 관리 중인 축제를 단건 조회한다.
     *
     * TODO(admin): 관리자 축제 참여 이력 테이블 도입 후 과거 이력 단건 조회까지 확장한다.
     */
    public AdminManagedFestivalView getManagedFestival(
            UUID festivalId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        return managedFestivalQueryService.getCurrentManagedFestival(
                adminAccount.getId(),
                festivalId,
                LocalDate.now(clock)
        );
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        return adminAccount;
    }
}
