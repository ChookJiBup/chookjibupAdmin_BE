package com.example.chookjibupadmin.map.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisJobService;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.query.application.dto.MapAnalysisStatusView;
import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import com.example.chookjibupadmin.map.query.application.dto.RoadmapNodeView;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        MapAnalysisJob job = jobService.getLatestByMapId(map.getId());
        MapImageReadUrl url = storagePort.createReadUrl(
                map.getDisplayImageKey().getValue()
        );
        List<RoadmapNodeView> nodes = nodeService.findAll(
                roadmap.getId(),
                map.getId()
        ).stream().map(this::view).toList();

        return new MapEditorView(
                map.getPublicId(),
                url.url(),
                url.expiresAt(),
                map.getDisplayImageDimensions().getWidth(),
                map.getDisplayImageDimensions().getHeight(),
                roadmap.getEditRevision(),
                roadmap.getStatus().name(),
                status(job),
                nodes
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
                    node.getSortOrder()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Stored roadmap geometry is invalid",
                    exception
            );
        }
    }
}
