package com.example.chookjibupadmin.map.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisJobService;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.query.application.dto.MapAnalysisStatusView;
import com.example.chookjibupadmin.map.query.application.dto.MapCenterView;
import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import com.example.chookjibupadmin.map.query.application.dto.RoadmapNodeView;
import com.example.chookjibupadmin.map.query.application.dto.RoadmapZoneView;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalMapAnalysisQueryApplicationService {

    private static final TypeReference<Map<String, Object>> GEOMETRY_TYPE =
            new TypeReference<>() {
            };

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;
    private final FestivalLocationService festivalLocationService;
    private final FestivalMapService mapService;
    private final MapAnalysisJobService jobService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService nodeService;
    private final MapImageStoragePort storagePort;
    private final ObjectMapper objectMapper;

    public MapAnalysisStatusView status(
            UUID festivalId,
            UUID mapId,
            AdminPrincipal principal
    ) {
        FestivalMap map = authorize(festivalId, mapId, principal);
        return status(jobService.getLatestByMapId(map.getId()));
    }

    public MapEditorView editor(
            UUID festivalId,
            UUID mapId,
            AdminPrincipal principal
    ) {
        FestivalMap map = authorize(festivalId, mapId, principal);
        map.validateReadable();

        FestivalRoadmap roadmap = roadmapService.getByFestivalId(
                map.getFestivalId()
        );
        if (!roadmap.getCurrentMapId().equals(map.getId())) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_MAP_INVALID_STATUS
            );
        }

        List<RoadmapNodeView> nodes = nodeService.findAll(
                roadmap.getId(),
                map.getId()
        ).stream().map(this::view).toList();

        MapCenterView center = resolveCenter(map.getFestivalId(), nodes);

        if (map.isCoordinateMap()) {
            return new MapEditorView(
                    map.getPublicId(),
                    null,
                    null,
                    0,
                    0,
                    roadmap.getEditRevision(),
                    roadmap.getStatus().name(),
                    null,
                    nodes,
                    zoneViews(roadmap),
                    center
            );
        }

        MapAnalysisJob job = jobService.getLatestByMapId(map.getId());
        MapImageReadUrl url = storagePort.createReadUrl(
                map.getDisplayImageKey().getValue()
        );

        return new MapEditorView(
                map.getPublicId(),
                url.url(),
                url.expiresAt(),
                map.getDisplayImageDimensions().getWidth(),
                map.getDisplayImageDimensions().getHeight(),
                roadmap.getEditRevision(),
                roadmap.getStatus().name(),
                status(job),
                nodes,
                zoneViews(roadmap),
                center
        );
    }

    private List<RoadmapZoneView> zoneViews(FestivalRoadmap roadmap) {
        return roadmap.getZones().stream()
                .map(zone -> new RoadmapZoneView(
                        zone.zoneId(), zone.name(), zone.sortOrder(), zone.boothNodeIds()))
                .toList();
    }

    private MapCenterView resolveCenter(Long festivalId, List<RoadmapNodeView> nodes) {
        return festivalLocationService.findAllByFestivalId(festivalId).stream()
                .filter(FestivalLocation::isPrimary)
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .findFirst()
                .map(location -> new MapCenterView(location.getLatitude(), location.getLongitude()))
                .orElseGet(() -> averageNodeCenter(nodes));
    }

    private MapCenterView averageNodeCenter(List<RoadmapNodeView> nodes) {
        List<RoadmapNodeView> points = nodes.stream()
                .filter(node -> "POINT".equals(node.geometryType()))
                .filter(node -> node.geometry().get("lat") instanceof Number number
                        && node.geometry().get("lng") instanceof Number)
                .toList();
        if (points.isEmpty()) {
            return null;
        }
        double latSum = 0;
        double lngSum = 0;
        for (RoadmapNodeView node : points) {
            latSum += ((Number) node.geometry().get("lat")).doubleValue();
            lngSum += ((Number) node.geometry().get("lng")).doubleValue();
        }
        return new MapCenterView(
                BigDecimal.valueOf(latSum / points.size()),
                BigDecimal.valueOf(lngSum / points.size())
        );
    }

    private FestivalMap authorize(
            UUID festivalPublicId,
            UUID mapPublicId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount admin = adminAccountService.getById(
                principal.adminId()
        );
        if (!admin.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }

        Festival festival = festivalService.getByPublicId(festivalPublicId);
        roleService.getByAdminAccountIdAndFestivalId(
                admin.getId(),
                festival.getId()
        );

        FestivalMap map = mapService.getByPublicId(mapPublicId);
        if (!map.belongsTo(festival.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }

        return map;
    }

    private MapAnalysisStatusView status(MapAnalysisJob job) {
        return new MapAnalysisStatusView(
                job.getPublicId(),
                job.getStatus().name(),
                job.getAttemptCount(),
                job.getDetectedCount(),
                job.getAcceptedCount(),
                job.getRejectedCount(),
                job.getFailureCode(),
                job.getFailureMessage(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }

    private RoadmapNodeView view(RoadmapNode node) {
        try {
            return new RoadmapNodeView(
                    node.getPublicId(),
                    node.getNodeType().name(),
                    node.getNodeName(),
                    node.getGeometryType().name(),
                    objectMapper.readValue(
                            node.getGeometryData(),
                            GEOMETRY_TYPE
                    ),
                    node.getConfidence(),
                    node.getRecognizedText(),
                    node.getSource().name(),
                    node.getReviewStatus().name(),
                    node.getSortOrder(),
                    node.getGeometrySchemaVersion()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Stored roadmap geometry is invalid",
                    exception
            );
        }
    }
}
