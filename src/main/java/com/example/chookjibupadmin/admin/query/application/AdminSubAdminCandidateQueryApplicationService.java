package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 제1 관리자의 서브관리자 초대 후보 조회 유스케이스를 처리한다.
 *
 * <p>클래스에 트랜잭션을 걸지 않는다. 검색어 매칭은 CPU 작업이라
 * 트랜잭션 안에서 돌리면 그동안 DB 커넥션을 쥐고 있게 되고, 검색어를 입력할 때마다
 * 요청이 쌓이면 커넥션 풀이 말라 다른 API까지 대기하기 때문이다.
 * 조회에 필요한 트랜잭션은 각 하위 Service가 스스로 연다.
 */
@Service
@RequiredArgsConstructor
public class AdminSubAdminCandidateQueryApplicationService {

    /** 자동완성 목록이므로 상위 후보만 내려 응답 크기를 제한한다. */
    private static final int MAX_RESULT_SIZE = 20;

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final AdminSubAdminCandidateQueryService candidateQueryService;
    private final AdminNameEmailSearchMatcher searchMatcher;

    /**
     * 제1 관리자가 초대 가능한 가입 관리자 후보를 검색한다.
     */
    public List<AdminSubAdminCandidateView> searchCandidates(
            UUID festivalId,
            String keyword,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateOwnerAccess(adminAccount.getId(), festival);

        // 이름 또는 이메일에 검색어가 포함된 후보만 남긴다.
        List<AdminSubAdminCandidateView> matched = searchMatcher.search(
                candidateQueryService.findCandidates(festival.getId()),
                keyword
        );

        return matched.size() <= MAX_RESULT_SIZE
                ? matched
                : List.copyOf(matched.subList(0, MAX_RESULT_SIZE));
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }

    private void validateOwnerAccess(
            Long adminAccountId,
            Festival festival
    ) {
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccountId,
                        festival.getId()
                );
        if (!role.canInviteSubAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
