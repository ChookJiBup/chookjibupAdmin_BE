package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.map.command.application.dto.FestivalMapDeletionTarget;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisQueueApplicationService;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치도 교체와 삭제 상태 전이를 DB 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalMapLifecycleApplicationService {

    private final FestivalMapService festivalMapService;
    private final FestivalService festivalService;
    private final FestivalLocationService festivalLocationService;
    private final FestivalRoadmapService festivalRoadmapService;
    private final RoadmapNodeService roadmapNodeService;
    private final MapAnalysisQueueApplicationService mapAnalysisQueueService;

    public FestivalMap replace(
            UUID currentMapId,
            Long festivalId,
            FestivalMap replacement
    ) {
        festivalService.getByIdForUpdate(festivalId);
        FestivalMap current = ownedMapForUpdate(currentMapId, festivalId);
        ensureNoApprovedBooth(festivalId, current);
        current.replaceWith(replacement, LocalDateTime.now());
        if (current.getLocationId() != null) {
            replacement.assignLocation(current.getLocationId());
        }
        assignDefaultAnchor(festivalId, replacement);
        FestivalMap saved = festivalMapService.save(replacement);
        mapAnalysisQueueService.enqueueReplacement(current, saved);
        return saved;
    }

    /**
     * 지도를 교체하면 노드 조회 스코프가 새 mapId로 옮겨가 옛 노드가 화면에서 사라진다.
     * booth_info는 roadmap_node_id를 필수로 들고 있고 바꿀 수도 없으므로, 승인된 부스가
     * 남아 있으면 부스 위치가 통째로 유실된다. 노드 승계 전까지는 교체 자체를 막는다.
     */
    private void ensureNoApprovedBooth(Long festivalId, FestivalMap current) {
        Optional<FestivalRoadmap> roadmap =
                festivalRoadmapService.findByFestivalId(festivalId);
        if (roadmap.isEmpty()) {
            return;
        }
        boolean hasApprovedBooth = roadmapNodeService
                .findAll(roadmap.get().getId(), current.getId())
                .stream()
                .map(RoadmapNode::getRelatedBoothId)
                .anyMatch(boothId -> boothId != null);
        if (hasApprovedBooth) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_MAP_REPLACE_BLOCKED_BY_BOOTH
            );
        }
    }

    /**
     * 앵커 조정 UI가 아직 없으므로, 축제 대표 위치를 이미지 중심으로 보는 기본 앵커를 부여한다.
     * 이 앵커가 있어야 AI가 찾은 정규화 좌표를 위경도로 옮겨 카카오맵 위에 그릴 수 있다.
     * 대표 위치에 위경도가 없으면 앵커 없이 두어 예전처럼 이미지 좌표(1.0)로 남긴다.
     */
    private void assignDefaultAnchor(Long festivalId, FestivalMap replacement) {
        festivalLocationService.findAllByFestivalId(festivalId).stream()
                .filter(FestivalLocation::isPrimary)
                .filter(location -> location.getLatitude() != null
                        && location.getLongitude() != null)
                .findFirst()
                .ifPresent(location -> replacement.assignImageAnchor(
                        MapImageAnchor.defaultAt(
                                location.getLatitude(),
                                location.getLongitude()
                        )
                ));
    }

    /**
     * 총괄관리자가 보정한 배치도 이미지 앵커를 저장한다.
     *
     * <p>좌표 전용 지도에는 얹을 이미지가 없어 앵커가 의미를 갖지 못하므로 거부한다.
     * 교체·삭제로 더 이상 현재본이 아닌 지도도 같은 이유로 막는다.</p>
     */
    public MapImageAnchor updateImageAnchor(
            UUID mapId,
            Long festivalId,
            MapImageAnchor anchor
    ) {
        FestivalMap festivalMap = ownedMapForUpdate(mapId, festivalId);
        if (festivalMap.isCoordinateMap()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        festivalMap.validateReadable();
        festivalMap.assignImageAnchor(anchor);
        return festivalMap.getImageAnchor();
    }

    public FestivalMapDeletionTarget beginDeletion(
            UUID mapId,
            Long festivalId
    ) {
        FestivalMap festivalMap = ownedMapForUpdate(mapId, festivalId);
        mapAnalysisQueueService.cancel(festivalMap);
        festivalMap.beginDeletion();
        return new FestivalMapDeletionTarget(
                festivalMap.getOriginalImageKey().getValue(),
                festivalMap.getDisplayImageKey().getValue(),
                festivalMap.getAnalysisImageKey().getValue()
        );
    }

    public void completeDeletion(UUID mapId, Long festivalId) {
        FestivalMap festivalMap = ownedMapForUpdate(mapId, festivalId);
        festivalMap.completeDeletion(LocalDateTime.now());
    }

    private FestivalMap ownedMapForUpdate(UUID mapId, Long festivalId) {
        FestivalMap festivalMap = festivalMapService.getByPublicIdForUpdate(mapId);
        if (!festivalMap.belongsTo(festivalId)) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }
        return festivalMap;
    }
}
