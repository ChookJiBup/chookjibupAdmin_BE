package com.example.chookjibupadmin.api.map.dto;
import com.example.chookjibupadmin.map.query.application.dto.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.*;
public record MapEditorResponse(UUID mapId,URI displayImageUrl,Instant displayImageUrlExpiresAt,
        int imageWidth,int imageHeight,long editRevision,String roadmapStatus,
        MapAnalysisStatusResponse analysis,List<NodeResponse> nodes){
    public static MapEditorResponse from(MapEditorView v){return new MapEditorResponse(v.mapId(),v.displayImageUrl(),
            v.displayImageUrlExpiresAt(),v.imageWidth(),v.imageHeight(),v.editRevision(),v.roadmapStatus(),
            MapAnalysisStatusResponse.from(v.analysis()),v.nodes().stream().map(NodeResponse::from).toList());}
    public record NodeResponse(UUID nodeId,String nodeType,String name,String geometryType,Map<String,Object> geometry,
            BigDecimal confidence,String recognizedText,String source,String reviewStatus,int sortOrder){
        static NodeResponse from(RoadmapNodeView v){return new NodeResponse(v.nodeId(),v.nodeType(),v.name(),
                v.geometryType(),v.geometry(),v.confidence(),v.recognizedText(),v.source(),v.reviewStatus(),v.sortOrder());}
    }
}
