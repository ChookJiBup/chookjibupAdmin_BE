package com.example.demoadmin.map.command.application;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import com.example.demoadmin.map.command.application.dto.MapAnalysisResultView;
import com.example.demoadmin.map.command.application.dto.PreparedMapAnalysis;
import com.example.demoadmin.map.command.application.port.DetectedMapObject;
import com.example.demoadmin.map.command.application.port.MapAnalysisResult;
import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import com.example.demoadmin.map.command.domain.FestivalMap;
import com.example.demoadmin.map.command.domain.MapAnalysisJob;
import com.example.demoadmin.map.command.domain.MapObject;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.example.demoadmin.map.command.domain.vo.ConfidenceScore;
import com.example.demoadmin.map.command.domain.vo.GeometryData;
import com.example.demoadmin.map.command.domain.vo.MapFileName;
import com.example.demoadmin.map.command.domain.vo.MapObjectName;
import com.example.demoadmin.map.command.domain.vo.MapStoragePath;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치도 분석 전후의 DB 상태 변경을 짧은 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class MapAnalysisPersistenceService {

    private final FestivalService festivalService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalMapService festivalMapService;
    private final MapAnalysisJobService mapAnalysisJobService;
    private final MapObjectService mapObjectService;

    /**
     * 관리자 권한을 검증하고 분석 대상 지도와 실행 중 작업을 저장한다.
     */
    @Transactional
    public PreparedMapAnalysis prepare(
            UUID festivalId,
            CreateTestMapAnalysisCommand command,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Festival festival = festivalService.getByPublicId(festivalId);
        validatePermission(festival.getId(), principal.adminId());
        FestivalMap festivalMap = festivalMapService.save(FestivalMap.create(
                festival.getId(),
                MapFileName.of(command.originalFileName()),
                MapStorageType.TEST_RESOURCE,
                MapStoragePath.of(command.storagePath()),
                command.width(),
                command.height()
        ));
        MapAnalysisJob analysisJob = mapAnalysisJobService.save(MapAnalysisJob.create(
                festivalMap.getId(),
                principal.adminId()
        ));
        analysisJob.start();

        return new PreparedMapAnalysis(
                festivalMap.getId(),
                festivalMap.getPublicId(),
                analysisJob.getId(),
                analysisJob.getPublicId(),
                new MapImageAnalysisRequest(
                        festivalMap.getPublicId(),
                        festivalMap.getStoragePathValue(),
                        festivalMap.getStorageType(),
                        festivalMap.getWidth(),
                        festivalMap.getHeight()
                )
        );
    }

    /**
     * 검증된 분석 결과를 지도 객체로 저장하고 분석 작업을 완료한다.
     */
    @Transactional
    public MapAnalysisResultView complete(
            PreparedMapAnalysis prepared,
            MapAnalysisResult result
    ) {
        FestivalMap festivalMap = festivalMapService.getById(prepared.festivalMapId());
        MapAnalysisJob analysisJob = mapAnalysisJobService.getById(
                prepared.analysisJobId()
        );
        List<MapObject> mapObjects = result.objects().stream()
                .map(object -> toMapObject(
                        festivalMap.getId(),
                        analysisJob.getId(),
                        object
                ))
                .toList();

        mapObjectService.saveAll(mapObjects);
        analysisJob.complete();
        festivalMap.markAnalyzed();

        return new MapAnalysisResultView(
                festivalMap.getPublicId(),
                analysisJob.getPublicId(),
                analysisJob.getStatus(),
                mapObjects.size()
        );
    }

    /**
     * 기존 분석 트랜잭션과 분리해 실패 상태와 사유를 저장한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            Long analysisJobId,
            String reason
    ) {
        MapAnalysisJob analysisJob = mapAnalysisJobService.getById(analysisJobId);
        analysisJob.fail(reason);
    }

    private void validatePermission(
            Long festivalId,
            Long adminId
    ) {
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(adminId, festivalId);
        if (!role.canManageQueueDesign()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private MapObject toMapObject(
            Long festivalMapId,
            Long analysisJobId,
            DetectedMapObject object
    ) {
        return MapObject.createAiGenerated(
                festivalMapId,
                analysisJobId,
                object.type(),
                MapObjectName.of(object.name()),
                object.geometryType(),
                GeometryData.of(object.geometryType(), object.geometryData()),
                ConfidenceScore.of(object.confidence())
        );
    }
}
