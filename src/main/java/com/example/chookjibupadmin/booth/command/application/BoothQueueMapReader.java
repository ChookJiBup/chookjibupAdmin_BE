package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.QueueGeometry;
import com.example.chookjibupadmin.booth.command.domain.QueuePlanConstraints;
import com.example.chookjibupadmin.map.command.application.FestivalMapPresentationService;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 지도 geometry JSON은 경계에서만 해석하며 정규화 좌표는 거리로 사용하지 않는다. */
@Component
@RequiredArgsConstructor
public class BoothQueueMapReader {
    private final RoadmapNodeService nodeService;
    private final ObjectMapper objectMapper;
    private final FestivalMapPresentationService presentationService;
    private final BoothQueuePlanService planService;
    private final BoothInfoService boothService;

    public RoadmapNode boothNode(BoothInfo booth) {
        if (booth.getRoadmapNodeId() == null) return null;
        return nodeService.findAllById(List.of(booth.getRoadmapNodeId())).stream().findFirst().orElse(null);
    }

    public Map<String, BigDecimal> boothPoint(BoothInfo booth) {
        var node = boothNode(booth);
        if (node == null || !"2.0".equals(node.getGeometrySchemaVersion())) return null;
        try {
            var geometry = objectMapper.readTree(node.getGeometryData());
            if (!geometry.path("lat").isNumber() || !geometry.path("lng").isNumber()) return null;
            return QueueGeometry.point(geometry.get("lat").decimalValue(), geometry.get("lng").decimalValue());
        } catch (Exception exception) { return null; }
    }

    public List<RoadmapNode> nodes(BoothInfo booth) {
        var node = boothNode(booth);
        if (node == null) throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        return nodeService.findAll(node.getRoadmapId(), node.getMapId());
    }

    public List<Map<String, BigDecimal>> points(String json) {
        try {
            var geometry = objectMapper.readTree(json);
            List<Map<String, BigDecimal>> points = new ArrayList<>();
            if (geometry.path("lat").isNumber() && geometry.path("lng").isNumber()) {
                points.add(QueueGeometry.point(geometry.get("lat").decimalValue(), geometry.get("lng").decimalValue()));
            } else {
                for (var p : geometry.path("points")) {
                    if (!p.path("lat").isNumber() || !p.path("lng").isNumber()) return List.of();
                    points.add(QueueGeometry.point(p.get("lat").decimalValue(), p.get("lng").decimalValue()));
                }
            }
            return List.copyOf(points);
        } catch (Exception exception) { return List.of(); }
    }

    public List<Map<String, BigDecimal>> boundary(BoothInfo booth) {
        var node = boothNode(booth);
        if (node == null) return null;
        var json = presentationService.findByMapId(node.getMapId())
                .map(p -> p.getBoundaryGeometry()).orElse(null);
        if (json == null) return null;
        var points = points(json);
        if (points.size() < 3) throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        return points;
    }

    public void validatePlan(BoothInfo booth, List<Map<String, BigDecimal>> path, UUID sourceNodeId) {
        var nodes = nodes(booth);
        if (sourceNodeId != null && nodes.stream().noneMatch(n -> n.getPublicId().equals(sourceNodeId)
                && n.getNodeType() == NodeType.QUEUE && n.getGeometryType() == GeometryType.POLYLINE
                && "2.0".equals(n.getGeometrySchemaVersion()))) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        List<QueuePlanConstraints.Obstacle> obstacles = new ArrayList<>();
        for (var node : nodes) {
            if (node.getId().equals(booth.getRoadmapNodeId()) || node.getNodeType() == NodeType.QUEUE
                    || !"2.0".equals(node.getGeometrySchemaVersion())) continue;
            var points = points(node.getGeometryData());
            if (!points.isEmpty()) obstacles.add(new QueuePlanConstraints.Obstacle(points,
                    node.getGeometryType() == GeometryType.POLYGON));
        }
        for (var other : boothService.findAllByFestivalId(booth.getFestivalId())) {
            if (other.getId().equals(booth.getId())) continue;
            planService.findByBoothId(other.getId()).ifPresent(p ->
                    obstacles.add(new QueuePlanConstraints.Obstacle(p.getPathGeometry(), false)));
        }
        QueuePlanConstraints.validate(path, boundary(booth), obstacles);
    }
}
