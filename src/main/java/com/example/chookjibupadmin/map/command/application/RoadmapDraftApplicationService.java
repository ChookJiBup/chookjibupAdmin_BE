package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.application.MapGeometryValidator;
import com.example.chookjibupadmin.map.analysis.application.MapBoundaryValidator;
import com.example.chookjibupadmin.map.analysis.application.MapBoundaryValidator.BoundaryPoint;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapNodeChangeCommand;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapZoneCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand.BoundaryGeometryCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand.LatLngPointCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand.OverlayPresentationCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.command.application.dto.SavedRoadmapDraft;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapZone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoadmapDraftApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService nodeService;
    private final FestivalMapPresentationService presentationService;
    private final MapGeometryValidator geometryValidator;
    private final MapBoundaryValidator boundaryValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public SavedRoadmapDraft save(
            UUID festivalPublicId,
            UUID mapPublicId,
            SaveRoadmapDraftCommand command,
            AdminPrincipal principal
    ) {
        AuthorizedEdit authorized = authorize(festivalPublicId, principal);
        FestivalMap map = mapService.getByPublicId(mapPublicId);
        if (!map.belongsTo(authorized.festivalId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }
        map.validateReadable();

        FestivalRoadmap roadmap = roadmapService.getByFestivalIdForUpdate(
                authorized.festivalId()
        );
        if (!roadmap.getCurrentMapId().equals(map.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        validateCommand(command);

        List<RoadmapNode> currentNodes = nodeService.findAll(
                roadmap.getId(),
                map.getId()
        );
        Map<UUID, RoadmapNode> currentNodeById = new HashMap<>();
        currentNodes.forEach(node -> currentNodeById.put(
                node.getPublicId(),
                node
        ));
        validateChanges(command.nodes(), currentNodeById, map.geometrySchemaVersion());
        long editRevision = roadmap.applyAdminEdit(command.baseRevision());

        List<RoadmapNode> changedNodes = new ArrayList<>();
        List<RoadmapNode> deletedNodes = new ArrayList<>();
        for (RoadmapNodeChangeCommand change : command.nodes()) {
            if (change.deleted()) {
                deletedNodes.add(currentNodeById.get(change.nodeId()));
            } else if (change.nodeId() == null) {
                changedNodes.add(createNode(
                        roadmap,
                        map,
                        change,
                        authorized.adminId(),
                        map.geometrySchemaVersion()
                ));
            } else {
                RoadmapNode node = currentNodeById.get(change.nodeId());
                node.updateByAdmin(
                        change.nodeType(),
                        change.name().trim(),
                        change.geometryType(),
                        geometryJson(change),
                        change.sortOrder(),
                        authorized.adminId()
                );
                changedNodes.add(node);
            }
        }

        if (!changedNodes.isEmpty()) {
            nodeService.saveAll(changedNodes);
        }
        if (!deletedNodes.isEmpty()) {
            nodeService.deleteAll(deletedNodes);
        }
        if (command.zones() != null) {
            roadmap.replaceZones(resolveZones(command.zones(), currentNodes, changedNodes, deletedNodes));
        } else if (!deletedNodes.isEmpty()) {
            pruneDeletedZoneMembers(roadmap, deletedNodes);
        }
        if (command.presentation() != null) {
            applyPresentation(map, command.presentation());
        }
        return new SavedRoadmapDraft(editRevision);
    }

    private void applyPresentation(
            FestivalMap map,
            SaveMapPresentationCommand command
    ) {
        FestivalMapPresentation presentation = presentationService
                .getOrCreateForUpdate(map.getId(), map.getFestivalId());

        if (Boolean.TRUE.equals(command.clearBoundary())) {
            presentation.clearBoundary();
        } else if (command.boundary() != null) {
            presentation.updateBoundary(boundaryJson(command.boundary()));
        }

        if (Boolean.TRUE.equals(command.clearOverlay())) {
            presentation.clearOverlay();
        } else if (command.overlay() != null) {
            applyOverlayPatch(presentation, command.overlay());
        }

        presentationService.save(presentation);
    }

    private void applyOverlayPatch(
            FestivalMapPresentation presentation,
            OverlayPresentationCommand overlay
    ) {
        if (overlay.assetId() != null) {
            if (!presentation.hasOverlayImage()
                    || !overlay.assetId().equals(presentation.getOverlayAssetId())) {
                throw new CustomException(ErrorCode.MAP_PRESENTATION_OVERLAY_INVALID);
            }
        }
        if (hasAnyAnchorField(overlay)) {
            if (overlay.centerLatitude() == null
                    || overlay.centerLongitude() == null
                    || overlay.groundWidthMeters() == null
                    || overlay.rotationDegrees() == null) {
                throw new CustomException(ErrorCode.MAP_PRESENTATION_OVERLAY_INVALID);
            }
            presentation.updateOverlayAnchor(MapImageAnchor.of(
                    overlay.centerLatitude(),
                    overlay.centerLongitude(),
                    overlay.groundWidthMeters(),
                    overlay.rotationDegrees()
            ));
        }
        if (overlay.opacity() != null) {
            presentation.setOverlayOpacity(overlay.opacity());
        }
        if (overlay.clipToBoundary() != null) {
            presentation.setClipToBoundary(overlay.clipToBoundary());
        }
        if (overlay.visible() != null) {
            presentation.setOverlayVisible(overlay.visible());
        }
    }

    private boolean hasAnyAnchorField(OverlayPresentationCommand overlay) {
        return overlay.centerLatitude() != null
                || overlay.centerLongitude() != null
                || overlay.groundWidthMeters() != null
                || overlay.rotationDegrees() != null;
    }

    private String boundaryJson(BoundaryGeometryCommand boundary) {
        if (boundary == null
                || !"POLYGON".equals(boundary.geometryType())
                || !"2.0".equals(boundary.schemaVersion())
                || boundary.points() == null) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
        List<BoundaryPoint> validated = boundaryValidator.validateClosedPolygon(
                boundary.points().stream()
                        .map(this::toBoundaryPoint)
                        .toList()
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("geometryType", "POLYGON");
        payload.put("schemaVersion", "2.0");
        payload.put(
                "points",
                validated.stream()
                        .map(point -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("lat", point.lat());
                            map.put("lng", point.lng());
                            return map;
                        })
                        .toList()
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
    }

    private BoundaryPoint toBoundaryPoint(LatLngPointCommand point) {
        if (point == null) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
        return new BoundaryPoint(point.lat(), point.lng());
    }

    private AuthorizedEdit authorize(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount admin = adminAccountService.getById(principal.adminId());
        if (!admin.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        if (festival.getStatus() != FestivalStatus.DRAFT) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        AdminFestivalRole role = roleService.getByAdminAccountIdAndFestivalId(
                admin.getId(),
                festival.getId()
        );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return new AuthorizedEdit(festival.getId(), admin.getId());
    }

    /**
     * 저장 요청의 최소 조건을 본다.
     *
     * <p>노드 목록은 비어 있어도 된다. 부지 경계나 팜플렛만 저장하는 경우가 있어서다 —
     * 예전에는 노드를 하나 이상 요구해, 핀을 아직 찍지 않은 지도에는 경계를 저장할 수
     * 없었다. 대신 노드도 표시 설정도 없는 요청은 할 일이 없으므로 거절한다.</p>
     */
    private void validateCommand(SaveRoadmapDraftCommand command) {
        if (command == null
                || command.baseRevision() < 0
                || command.nodes() == null
                || command.nodes().size() > 1000
                || (command.nodes().isEmpty() && command.presentation() == null)) {
            throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
        }
    }

    private void validateChanges(
            List<RoadmapNodeChangeCommand> changes,
            Map<UUID, RoadmapNode> currentNodeById,
            String geometrySchemaVersion
    ) {
        Set<UUID> requestedNodeIds = new HashSet<>();
        Set<UUID> clientNodeIds = new HashSet<>();
        for (RoadmapNodeChangeCommand change : changes) {
            if (change == null) {
                throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
            }
            if (change.nodeId() != null
                    && !requestedNodeIds.add(change.nodeId())) {
                throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
            }
            if (change.nodeId() != null
                    && !currentNodeById.containsKey(change.nodeId())) {
                throw new CustomException(ErrorCode.ROADMAP_NODE_NOT_FOUND);
            }
            if (change.clientNodeId() != null
                    && (change.nodeId() != null
                    || currentNodeById.containsKey(change.clientNodeId())
                    || !clientNodeIds.add(change.clientNodeId()))) {
                throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
            }
            if (change.deleted()) {
                if (change.nodeId() == null) {
                    throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
                }
                continue;
            }
            if (change.nodeType() == null
                    || change.name() == null
                    || change.name().isBlank()
                    || change.name().trim().length() > 150
                    || change.sortOrder() == null
                    || change.sortOrder() < 0
                    || !geometryValidator.isValid(
                            geometrySchemaVersion,
                            change.geometryType(),
                            change.geometry()
                    )) {
                throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
            }
        }
    }

    private RoadmapNode createNode(
            FestivalRoadmap roadmap,
            FestivalMap map,
            RoadmapNodeChangeCommand change,
            Long adminId,
            String geometrySchemaVersion
    ) {
        return RoadmapNode.admin(
                change.clientNodeId(),
                roadmap.getId(),
                map.getId(),
                change.nodeType(),
                change.name().trim(),
                change.geometryType(),
                geometryJson(change),
                change.sortOrder(),
                adminId,
                geometrySchemaVersion
        );
    }

    private List<RoadmapZone> resolveZones(
            List<RoadmapZoneCommand> zones,
            List<RoadmapNode> currentNodes,
            List<RoadmapNode> changedNodes,
            List<RoadmapNode> deletedNodes
    ) {
        if (zones.size() > 200) {
            throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
        }
        Set<UUID> deletedIds = deletedNodes.stream()
                .map(RoadmapNode::getPublicId)
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, RoadmapNode> available = new HashMap<>();
        currentNodes.stream().filter(node -> !deletedIds.contains(node.getPublicId()))
                .forEach(node -> available.put(node.getPublicId(), node));
        changedNodes.forEach(node -> available.put(node.getPublicId(), node));

        Set<UUID> zoneIds = new HashSet<>();
        Set<UUID> assignedBooths = new HashSet<>();
        List<RoadmapZone> resolved = new ArrayList<>();
        for (RoadmapZoneCommand zone : zones) {
            UUID zoneId = zone.zoneId() == null ? UUID.randomUUID() : zone.zoneId();
            if (!zoneIds.add(zoneId)
                    || zone.name() == null || zone.name().isBlank()
                    || zone.name().trim().length() > 100
                    || zone.sortOrder() == null || zone.sortOrder() < 0
                    || zone.boothNodeIds() == null || zone.boothNodeIds().isEmpty()) {
                throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
            }
            for (UUID boothId : zone.boothNodeIds()) {
                RoadmapNode booth = available.get(boothId);
                if (booth == null || booth.getNodeType() != com.example.chookjibupadmin.map.roadmap.domain.NodeType.BOOTH
                        || !assignedBooths.add(boothId)) {
                    throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
                }
            }
            resolved.add(new RoadmapZone(zoneId, zone.name().trim(), zone.sortOrder(), zone.boothNodeIds()));
        }
        return resolved;
    }

    private void pruneDeletedZoneMembers(
            FestivalRoadmap roadmap,
            List<RoadmapNode> deletedNodes
    ) {
        Set<UUID> deletedIds = deletedNodes.stream()
                .map(RoadmapNode::getPublicId)
                .collect(java.util.stream.Collectors.toSet());
        roadmap.replaceZones(roadmap.getZones().stream()
                .map(zone -> new RoadmapZone(
                        zone.zoneId(),
                        zone.name(),
                        zone.sortOrder(),
                        zone.boothNodeIds().stream()
                                .filter(id -> !deletedIds.contains(id))
                                .toList()))
                .filter(zone -> !zone.boothNodeIds().isEmpty())
                .toList());
    }

    private String geometryJson(RoadmapNodeChangeCommand change) {
        try {
            return objectMapper.writeValueAsString(change.geometry());
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
        }
    }

    private record AuthorizedEdit(Long festivalId, Long adminId) {
    }
}
