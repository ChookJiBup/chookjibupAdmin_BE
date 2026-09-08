package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.ApproveBoothResult;
import com.example.chookjibupadmin.booth.command.application.dto.ApproveBoothsResult;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지도 부스 노드를 승인하여 booth_info를 생성한다.
 */
@Slf4j
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
    private final FestivalRoadmapService festivalRoadmapService;

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

    /**
     * 지도에 찍힌 부스 노드를 한 번에 승인한다.
     *
     * <p>지도를 저장해도 운영 부스가 만들어지지 않아 대시보드에 «부스 0개»로 보였다.
     * 노드마다 승인 API를 부르게 하면 부스 수만큼 왕복이 생기므로 한 번에 처리한다.
     * 이미 승인된 노드와 부스가 아닌 노드는 조용히 건너뛴다 — 저장할 때마다 부르는
     * 경로라 «이미 승인됨»으로 실패하면 안 된다.</p>
     */
    public ApproveBoothsResult approveAll(
            UUID festivalPublicId,
            UUID mapPublicId,
            AdminPrincipal principal
    ) {
        AdminAccount admin = requireAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        requireFestivalManage(admin.getId(), festival.getId());

        FestivalMap map = festivalMapService.getByPublicId(mapPublicId);
        if (!map.belongsTo(festival.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }

        FestivalRoadmap roadmap = festivalRoadmapService.getByFestivalId(festival.getId());
        List<RoadmapNode> approvable = roadmapNodeService
                .findAll(roadmap.getId(), map.getId()).stream()
                .filter(node -> node.getNodeType() == NodeType.BOOTH)
                .filter(node -> node.getRelatedBoothId() == null)
                .toList();

        List<ApproveBoothResult> approved = new ArrayList<>();
        for (RoadmapNode node : approvable) {
            try {
                BoothInfo booth = boothInfoService.save(
                        BoothInfo.create(festival.getId(), node.getId(), node.getNodeName())
                );
                node.approveBooth(booth.getId(), admin.getId());
                roadmapNodeService.save(node);
                approved.add(new ApproveBoothResult(
                        booth.getId(),
                        node.getPublicId(),
                        booth.getBoothName()
                ));
            } catch (DataIntegrityViolationException exception) {
                // 다른 요청이 먼저 승인했다. 이 노드만 건너뛴다.
                log.info("Booth already approved for node={}", node.getPublicId());
            }
        }
        return new ApproveBoothsResult(approved);
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
