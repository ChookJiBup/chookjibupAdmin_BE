package com.example.chookjibupadmin.operator.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자와 현장 스태프의 공통 축제 운영 기능 접근 범위를 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalOperationAccessService {

    private final FestivalService festivalService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FieldStaffAccountService fieldStaffAccountService;
    private final Clock clock;

    /**
     * 인증 주체가 지정 축제의 현장 운영 기능을 사용할 수 있는지 확인한다.
     *
     * @return 내부 도메인 조회에 사용할 축제 PK
     */
    public Long getAuthorizedFestivalId(
            UUID festivalPublicId,
            FestivalActorPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Festival festival = festivalService.getByPublicId(festivalPublicId);
        if (principal instanceof AdminPrincipal adminPrincipal) {
            validateAdmin(adminPrincipal, festival.getId());
            return festival.getId();
        }
        if (principal instanceof FieldStaffPrincipal fieldStaffPrincipal) {
            validateFieldStaff(fieldStaffPrincipal, festival.getId());
            return festival.getId();
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    private void validateAdmin(
            AdminPrincipal principal,
            Long festivalId
    ) {
        if (!adminAccountService.isAuthenticationValid(
                principal.adminId(),
                principal.authVersion()
        )) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                principal.adminId(),
                festivalId
        );
    }

    private void validateFieldStaff(
            FieldStaffPrincipal principal,
            Long festivalId
    ) {
        fieldStaffAccountService.validateAuthentication(
                principal,
                LocalDateTime.now(clock)
        );
        if (!festivalId.equals(principal.festivalId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
