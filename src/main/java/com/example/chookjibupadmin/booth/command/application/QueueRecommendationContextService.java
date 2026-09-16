package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.auth.support.*;
import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort.Input;
import com.example.chookjibupadmin.booth.command.domain.*;
import com.example.chookjibupadmin.global.response.*;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 추천 전후의 짧은 읽기 트랜잭션이다. 외부 모델 호출 중 DB 연결을 점유하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueRecommendationContextService {
    private final FestivalOperationAccessService accessService;
    private final AdminFestivalRoleService roleService;
    private final BoothInfoService boothService;
    private final BoothQueuePlanService planService;
    private final BoothQueueMapReader mapReader;
    private final QueueWriteAccess writeAccess;
    public record Context(Input input, Long nodeVersion, long planRevision) {}
    public record LineCandidate(UUID sourceNodeId, String name, String source,
            List<Map<String, BigDecimal>> path, BigDecimal confidence, Long expectedNodeVersion,
            long expectedRevision) {}

    public List<LineCandidate> candidates(UUID festivalId, Long boothId, FestivalActorPrincipal principal) {
        var booth = authorized(festivalId, boothId, principal);
        var boothNode = mapReader.boothNode(booth);
        long revision = planService.findByBoothId(boothId).map(BoothQueuePlan::getRevision).orElse(0L);
        return mapReader.nodes(booth).stream().filter(n -> n.getNodeType()
                == com.example.chookjibupadmin.map.roadmap.domain.NodeType.QUEUE)
                .filter(n -> n.getGeometryType() == com.example.chookjibupadmin.map.roadmap.domain.GeometryType.POLYLINE)
                .filter(n -> "2.0".equals(n.getGeometrySchemaVersion()))
                .map(n -> new LineCandidate(n.getPublicId(), n.getNodeName(), n.getSource().name(),
                        mapReader.points(n.getGeometryData()), n.getConfidence(), boothNode.getVersion(), revision))
                .filter(c -> c.path().size() >= 2).toList();
    }

    public Context read(UUID festivalId, Long boothId, int capacity, double spacing,
            FestivalActorPrincipal principal) {
        var booth = authorized(festivalId, boothId, principal);
        if (capacity < 1 || capacity > 1000) throw new CustomException(ErrorCode.INVALID_REQUEST);
        QueueEstimationSettings.of(spacing, 2);
        var start = mapReader.boothPoint(booth);
        var boundary = mapReader.boundary(booth);
        if (start == null || boundary == null) throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        var nodes = mapReader.nodes(booth);
        if (nodes.size() > 500) throw new CustomException(ErrorCode.INVALID_REQUEST);
        var facilities = new java.util.ArrayList<>(nodes.stream().filter(n -> !n.getId().equals(booth.getRoadmapNodeId()))
                .filter(n -> "2.0".equals(n.getGeometrySchemaVersion()))
                .map(n -> Map.<String, Object>of("type", n.getNodeType().name(), "points", mapReader.points(n.getGeometryData())))
                .toList());
        for (var other : boothService.findAllByFestivalId(booth.getFestivalId())) {
            if (!other.getId().equals(boothId)) planService.findByBoothId(other.getId()).ifPresent(p ->
                    facilities.add(Map.of("type", "QUEUE_PLAN", "points", p.getPathGeometry())));
        }
        var plan = planService.findByBoothId(boothId);
        return new Context(new Input(start, boundary, facilities, capacity, spacing),
                mapReader.boothNode(booth).getVersion(), plan.map(BoothQueuePlan::getRevision).orElse(0L));
    }

    public void validate(UUID festivalId, Long boothId, Context context,
            List<Map<String, BigDecimal>> path, FestivalActorPrincipal principal) {
        var booth = authorized(festivalId, boothId, principal);
        if (!context.nodeVersion().equals(mapReader.boothNode(booth).getVersion())
                || context.planRevision() != planService.findByBoothId(boothId).map(BoothQueuePlan::getRevision).orElse(0L)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT);
        }
        QueueGeometry.validate(path);
        double targetLength = context.input().targetCapacity() * context.input().metersPerPerson();
        if (QueueGeometry.length(path) < targetLength * 0.5 || QueueGeometry.length(path) > targetLength * 1.5) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        if (QueueGeometry.distance(context.input().start(), path.getFirst()) > 2) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        mapReader.validatePlan(booth, path, null);
    }

    private BoothInfo authorized(UUID festivalId, Long boothId, FestivalActorPrincipal principal) {
        Long id = accessService.getAuthorizedFestivalId(festivalId, principal);
        if (!(principal instanceof AdminPrincipal admin)
                || !roleService.getByAdminAccountIdAndFestivalId(admin.adminId(), id).canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        writeAccess.requireOpen(festivalId);
        var booth = boothService.getById(boothId);
        if (!booth.belongsTo(id)) throw new CustomException(ErrorCode.BOOTH_NOT_FOUND);
        return booth;
    }
}
