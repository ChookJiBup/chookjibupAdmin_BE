package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.ApproveBoothResult;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지도 부스 노드를 승인하여 booth_info를 생성한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BoothApprovalApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalMapService festivalMapService;
    private final RoadmapNodeService roadmapNodeService;
    private final BoothInfoService boothInfoService;

    public ApproveBoothResult approve(
            UUID festivalPublicId,
            UUID mapPublicId,
            UUID nodePublicId,
            AdminPrincipal principal
    ) {
        AdminAccount admin = requireAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        requireFestivalManage(admin.getId(), festival.getId());

        FestivalMap map = festivalMapService.getByPublicId(mapPublicId);
        if (!map.belongsTo(festival.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }

        RoadmapNode node = roadmapNodeService.getByPublicIdAndMapIdForUpdate(
                nodePublicId,
                map.getId()
        );
        node.ensureBoothApprovable();
        if (boothInfoService.findByFestivalIdAndRoadmapNodeId(
                festival.getId(),
                node.getId()
        ).isPresent()) {
            throw new CustomException(ErrorCode.ROADMAP_NODE_ALREADY_APPROVED);
        }

        try {
            BoothInfo booth = boothInfoService.save(
                    BoothInfo.create(festival.getId(), node.getId(), node.getNodeName())
            );
            node.approveBooth(booth.getId(), admin.getId());
            roadmapNodeService.save(node);
            return new ApproveBoothResult(
                    booth.getId(),
                    node.getPublicId(),
                    booth.getBoothName()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(ErrorCode.ROADMAP_NODE_ALREADY_APPROVED);
        }
    }

    private AdminAccount requireAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }

    private void requireFestivalManage(Long adminId, Long festivalId) {
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(adminId, festivalId);
        if (!role.canManageQueueDesign()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
