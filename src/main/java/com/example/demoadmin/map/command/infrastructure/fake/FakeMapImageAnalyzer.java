package com.example.demoadmin.map.command.infrastructure.fake;

import com.example.demoadmin.map.command.application.port.DetectedMapObject;
import com.example.demoadmin.map.command.application.port.MapAnalysisResult;
import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import com.example.demoadmin.map.command.application.port.MapImageAnalyzer;
import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapObjectType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 외부 호출 없이 배치도 분석 저장 흐름을 검증하는 개발·테스트용 analyzer이다.
 */
@Component
@ConditionalOnProperty(
        name = "app.map.analysis.provider",
        havingValue = "fake",
        matchIfMissing = true
)
public class FakeMapImageAnalyzer implements MapImageAnalyzer {

    @Override
    public MapAnalysisResult analyze(MapImageAnalysisRequest request) {
        return new MapAnalysisResult(List.of(
                new DetectedMapObject(
                        MapObjectType.FOOD_BOOTH,
                        "김밥 부스",
                        GeometryType.RECTANGLE,
                        "{\"type\":\"RECTANGLE\",\"x\":0.31,\"y\":0.22,\"width\":0.08,\"height\":0.05}",
                        0.82
                ),
                new DetectedMapObject(
                        MapObjectType.ENTRANCE,
                        "입구",
                        GeometryType.POINT,
                        "{\"type\":\"POINT\",\"x\":0.12,\"y\":0.88}",
                        0.76
                )
        ));
    }
}
