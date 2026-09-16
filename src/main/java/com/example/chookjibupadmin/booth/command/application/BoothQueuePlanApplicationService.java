package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.*;
import com.example.chookjibupadmin.booth.command.domain.*;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사전 동선 조회/저장. 실제 줄과 혼잡 이력은 수정하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueuePlanApplicationService {
    private final FestivalOperationAccessService accessService;
    private final AdminFestivalRoleService roleService;
    private final BoothInfoService boothService;
    private final BoothQueuePlanService planService;
    private final BoothQueueMapReader mapReader;
    private final QueueWriteAccess writeAccess;
    private final com.example.chookjibupadmin.festival.command.application.FestivalService festivalService;
    private final FestivalRoadmapService roadmapService;

    public QueuePlanView get(UUID festivalId, Long boothId, FestivalActorPrincipal principal) {
        Long id = accessService.getAuthorizedFestivalId(festivalId, principal);
        var booth = boothService.getById(boothId);
        if (!booth.belongsTo(id)) throw new CustomException(ErrorCode.BOOTH_NOT_FOUND);
        var plan = planService.findByBoothId(boothId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_QUEUE_PLAN_NOT_FOUND));
        var node = mapReader.boothNode(booth);
        return QueuePlanView.from(plan, node == null ? null : node.getVersion());
    }

    @Transactional
    public QueuePlanView save(UUID festivalId, Long boothId, SaveQueuePlanCommand command,
            FestivalActorPrincipal principal) {
        Long id = accessService.getAuthorizedFestivalId(festivalId, principal);
        if (!(principal instanceof AdminPrincipal admin)
                || !roleService.getByAdminAccountIdAndFestivalId(admin.adminId(), id).canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        writeAccess.requireOpen(festivalId);
        // 다른 부스의 계획끼리 동시에 교차 경로를 확정하지 않도록 축제 단위로 직렬화한다.
        festivalService.getByIdForUpdate(id);
        // 지도 초안 저장과 같은 잠금으로 검증 도중 시설과 부스 좌표가 바뀌지 않게 한다.
        var roadmap = roadmapService.getByFestivalIdForUpdate(id);
        var booth = boothService.getByIdForUpdate(boothId);
        if (!booth.belongsTo(id)) throw new CustomException(ErrorCode.BOOTH_NOT_FOUND);
        var node = mapReader.boothNode(booth);
        if (node != null && !node.getMapId().equals(roadmap.getCurrentMapId())) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT);
        }
        if (command.expectedNodeVersion() == null || node == null
                || !command.expectedNodeVersion().equals(node.getVersion())) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT);
        }
        var point = mapReader.boothPoint(booth);
        var path = QueueGeometry.validate(command.path());
        if (point == null || QueueGeometry.distance(point, path.getFirst()) > 10) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        mapReader.validatePlan(booth, path, command.sourceNodeId());
        var plan = planService.findByBoothId(boothId).orElseGet(() -> BoothQueuePlan.create(id, boothId));
        plan.replace(path, QueueEstimationSettings.of(command.metersPerPerson(), command.servedPersonsPerMinute()),
                command.sourceNodeId(), admin.adminId(), command.expectedRevision());
        return QueuePlanView.from(planService.save(plan), node.getVersion());
    }
}
