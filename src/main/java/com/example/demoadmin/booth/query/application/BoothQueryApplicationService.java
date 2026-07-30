package com.example.demoadmin.booth.query.application;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 권한의 축제 부스 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueryApplicationService {

    private final FestivalService festivalService;
    private final BoothQueryService boothQueryService;
    private final AdminFestivalRoleService adminFestivalRoleService;

    public List<BoothView> getBooths(
            UUID festivalId,
            AdminPrincipal principal
    ) {
        Festival festival = findFestivalAndValidate(festivalId, principal);
        return boothQueryService.findAllByFestivalId(festival.getId());
    }

    public BoothView getBooth(
            UUID festivalId,
            UUID boothId,
            AdminPrincipal principal
    ) {
        Festival festival = findFestivalAndValidate(festivalId, principal);
        return boothQueryService.getByFestivalIdAndPublicId(
                festival.getId(),
                boothId
        );
    }

    public List<BoothQueueLineView> getQueueLines(
            UUID festivalId,
            UUID boothId,
            AdminPrincipal principal
    ) {
        Festival festival = findFestivalAndValidate(festivalId, principal);
        boothQueryService.getByFestivalIdAndPublicId(
                festival.getId(),
                boothId
        );

        return boothQueryService.findQueueLinesByFestivalIdAndBoothPublicId(
                festival.getId(),
                boothId
        );
    }

    private Festival findFestivalAndValidate(
            UUID festivalId,
            AdminPrincipal principal
    ) {
        Festival festival = festivalService.getByPublicId(festivalId);
        AdminFestivalRole role = getRole(festival.getId(), principal);
        if (!role.canManageQueueDesign()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return festival;
    }

    private AdminFestivalRole getRole(
            Long festivalId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                principal.adminId(),
                festivalId
        );
    }
}
