package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 제1관리자가 활성 관리자 계정을 담당 축제의 제2관리자로 배정한다.
 *
 * <p>배정 쓰기는 {@link AdminFestivalRoleService} 안에서만 트랜잭션을 연다.
 * 유니크 제약 충돌을 트랜잭션 밖에서 잡아야 롤백된 세션을 다시 쓰지 않고
 * 이미 저장된 결과를 다시 읽어 성공으로 응답할 수 있기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class AdminSubAdminAssignService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;

    /**
     * 요청 관리자의 제1관리자 권한을 확인하고 대상 계정에 축제 역할을 부여한다.
     */
    public AdminFestivalRole assign(
            UUID festivalId,
            UUID targetAdminId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount owner = adminAccountService.getById(principal.adminId());
        if (!owner.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalId);
        AdminFestivalRole ownerRole = roleService
                .getByAdminAccountIdAndFestivalId(owner.getId(), festival.getId());
        if (!ownerRole.canInviteSubAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        AdminAccount target = adminAccountService.findByPublicId(targetAdminId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND
                ));
        if (!target.isActive() || target.getId().equals(owner.getId())) {
            throw new CustomException(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND);
        }
        if (!target.isContractor()) {
            throw new CustomException(ErrorCode.AUTH_GOVERNMENT_ACCOUNT_CANNOT_BE_OPERATOR);
        }
        return assignOrReuse(target.getId(), festival.getId(), owner.getId());
    }

    /**
     * 배정을 시도하고, 동시 요청이 먼저 커밋해 제약 위반이 나면 저장된 결과를 돌려준다.
     *
     * <p>이렇게 해야 서버에는 반영됐는데 화면에는 실패로 보이는 상태가 생기지 않는다.
     */
    private AdminFestivalRole assignOrReuse(
            Long targetAccountId,
            Long festivalId,
            Long ownerAccountId
    ) {
        try {
            return roleService.assignSubAdmin(
                    targetAccountId,
                    festivalId,
                    ownerAccountId
            );
        } catch (DataIntegrityViolationException exception) {
            return roleService.getAssignedSubAdmin(
                    targetAccountId,
                    festivalId,
                    ownerAccountId
            );
        }
    }
}
