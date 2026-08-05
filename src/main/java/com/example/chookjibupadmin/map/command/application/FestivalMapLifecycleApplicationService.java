package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.map.command.application.dto.FestivalMapDeletionTarget;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisQueueApplicationService;
import java.time.LocalDateTime;
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
    private final MapAnalysisQueueApplicationService mapAnalysisQueueService;

    public FestivalMap replace(
            UUID currentMapId,
            Long festivalId,
            FestivalMap replacement
    ) {
        festivalService.getByIdForUpdate(festivalId);
        FestivalMap current = ownedMapForUpdate(currentMapId, festivalId);
        current.replaceWith(replacement, LocalDateTime.now());
        if (current.getLocationId() != null) {
            replacement.assignLocation(current.getLocationId());
        }
        FestivalMap saved = festivalMapService.save(replacement);
        mapAnalysisQueueService.enqueueReplacement(current, saved);
        return saved;
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
