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
import com.example.chookjibupadmin.map.command.application.dto.RoadmapNodeChangeCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.command.application.dto.SavedRoadmapDraft;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private final MapGeometryValidator geometryValidator;
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
        validateChanges(command.nodes(), currentNodeById);
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
                        authorized.adminId()
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
        return new SavedRoadmapDraft(editRevision);
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

    private void validateCommand(SaveRoadmapDraftCommand command) {
        if (command == null
                || command.baseRevision() < 0
                || command.nodes() == null
                || command.nodes().isEmpty()
                || command.nodes().size() > 1000) {
            throw new CustomException(ErrorCode.ROADMAP_NODE_INVALID);
        }
    }

    private void validateChanges(
            List<RoadmapNodeChangeCommand> changes,
            Map<UUID, RoadmapNode> currentNodeById
    ) {
        Set<UUID> requestedNodeIds = new HashSet<>();
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
            Long adminId
    ) {
        return RoadmapNode.admin(
                roadmap.getId(),
                map.getId(),
                change.nodeType(),
                change.name().trim(),
                change.geometryType(),
                geometryJson(change),
                change.sortOrder(),
                adminId
        );
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
