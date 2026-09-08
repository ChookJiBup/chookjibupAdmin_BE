package com.example.chookjibupadmin.map.query.application;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapPresentationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.query.application.dto.FestivalOperationsMapView;
import com.example.chookjibupadmin.map.query.application.dto.FestivalOperationsMapView.ApprovedBoothMarkerView;
import com.example.chookjibupadmin.map.query.application.dto.FestivalOperationsMapView.FacilityMarkerView;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeReviewStatus;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 운영용 현재 지도와 승인 부스·시설 마커를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalOperationsMapQueryApplicationService {

    private static final TypeReference<Map<String, Object>> GEOMETRY_TYPE =
            new TypeReference<>() {
            };

    private final FestivalOperationAccessService festivalOperationAccessService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService nodeService;
    private final FestivalMapPresentationService presentationService;
    private final MapPresentationViewAssembler presentationViewAssembler;
    private final ObjectMapper objectMapper;

    public FestivalOperationsMapView getMap(
            UUID festivalPublicId,
            FestivalActorPrincipal principal
    ) {
        Long festivalId = festivalOperationAccessService.getAuthorizedFestivalId(
                festivalPublicId,
                principal
        );
        FestivalRoadmap roadmap = roadmapService.getByFestivalId(festivalId);
        FestivalMap map = mapService.getById(roadmap.getCurrentMapId());
        map.validateReadable();

        MapPresentationView presentation = presentationService.findByMapId(map.getId())
                .map(presentationViewAssembler::toView)
                .orElse(null);

        List<ApprovedBoothMarkerView> booths = new ArrayList<>();
        List<FacilityMarkerView> facilities = new ArrayList<>();
        for (RoadmapNode node : nodeService.findAll(roadmap.getId(), map.getId())) {
            if (node.getRelatedBoothId() != null) {
                PointLatLng point = readPoint(node);
                if (point == null) {
                    continue;
                }
                booths.add(new ApprovedBoothMarkerView(
                        node.getRelatedBoothId(),
                        node.getPublicId(),
                        node.getNodeName(),
                        node.getNodeType().name(),
                        point.lat(),
                        point.lng()
                ));
                continue;
            }
            if (!isFacility(node)) {
                continue;
            }
            PointLatLng point = readPoint(node);
            if (point == null) {
                continue;
            }
            facilities.add(new FacilityMarkerView(
                    node.getPublicId(),
                    node.getNodeName(),
                    node.getNodeType().name(),
                    point.lat(),
                    point.lng()
            ));
        }

        return new FestivalOperationsMapView(
                map.getPublicId(),
                roadmap.getEditRevision(),
                map.getMapKind().name(),
                presentation,
                List.copyOf(booths),
                List.copyOf(facilities)
        );
    }

    /**
     * 시설 마커로 내려보낼 노드인지 판단한다.
     *
     * BOOTH 유형은 제외한다. 승인 전 부스 후보까지 시설로 내보내면 운영 화면에 아직
     * 부스가 아닌 점이 찍힌다. AI가 찾아만 두고 아직 확정하지 않은 노드
     * (REVIEW_REQUIRED)도 제외한다. 운영 화면은 편집기와 달리 초안을 보여주는
     * 자리가 아니다.
     */
    private boolean isFacility(RoadmapNode node) {
        return node.getNodeType() != NodeType.BOOTH
                && node.getReviewStatus() == NodeReviewStatus.CONFIRMED;
    }

    private PointLatLng readPoint(RoadmapNode node) {
        if (node.getGeometryType() != GeometryType.POINT) {
            return null;
        }
        try {
            Map<String, Object> geometry = objectMapper.readValue(
                    node.getGeometryData(),
                    GEOMETRY_TYPE
            );
            Object lat = geometry.get("lat");
            Object lng = geometry.get("lng");
            if (!(lat instanceof Number latNumber) || !(lng instanceof Number lngNumber)) {
                return null;
            }
            return new PointLatLng(
                    BigDecimal.valueOf(latNumber.doubleValue()),
                    BigDecimal.valueOf(lngNumber.doubleValue())
            );
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private record PointLatLng(BigDecimal lat, BigDecimal lng) {
    }
}
