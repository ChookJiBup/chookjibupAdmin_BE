package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.map.analysis.application.MapAnalysisJobService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 삭제 과정에서 배치도 파일과 지도 데이터의 삭제 순서를 관리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalMapPurgeService {

    private final FestivalMapService festivalMapService;
    private final MapAnalysisJobService mapAnalysisJobService;
    private final FestivalRoadmapService festivalRoadmapService;
    private final RoadmapNodeService roadmapNodeService;

    /**
     * 분석 작업을 취소하고 배치도를 삭제 중 상태로 바꾼 뒤 파일 키를 반환한다.
     */
    public List<String> beginDeletion(Long festivalId) {
        List<FestivalMap> festivalMaps =
                festivalMapService.getAllByFestivalIdForUpdate(festivalId);

        festivalMaps.forEach(festivalMap -> {
            mapAnalysisJobService.cancelActive(festivalMap.getId());
            festivalMap.beginDeletion();
        });

        return festivalMaps.stream()
                .flatMap(festivalMap -> Stream.of(
                        festivalMap.getOriginalImageKey().getValue(),
                        festivalMap.getDisplayImageKey().getValue(),
                        festivalMap.getAnalysisImageKey().getValue()
                ))
                .distinct()
                .toList();
    }

    /**
     * 지도 데이터를 참조 순서의 역순으로 영구 삭제한다.
     */
    public void purgeDatabase(Long festivalId) {
        festivalRoadmapService.findByFestivalId(festivalId)
                .ifPresent(this::deleteRoadmap);

        List<FestivalMap> festivalMaps =
                festivalMapService.getAllByFestivalIdForUpdate(festivalId);
        List<Long> mapIds = festivalMaps.stream()
                .map(FestivalMap::getId)
                .toList();

        mapAnalysisJobService.deleteAllByMapIds(mapIds);
        festivalMapService.deleteAll(festivalMaps);
    }

    private void deleteRoadmap(FestivalRoadmap roadmap) {
        roadmapNodeService.deleteAllByRoadmapId(roadmap.getId());
        festivalRoadmapService.delete(roadmap);
    }
}
