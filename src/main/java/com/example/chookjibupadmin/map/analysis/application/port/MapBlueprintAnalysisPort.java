package com.example.chookjibupadmin.map.analysis.application.port;

import com.example.chookjibupadmin.map.analysis.application.dto.MapAnalysisResult;

/**
 * 축제 도면 이미지를 분석하여 편집 가능한 정규화 좌표 노드로 변환한다.
 */
public interface MapBlueprintAnalysisPort {

    /**
     * 지정한 이미지의 시설과 동선을 분석한다.
     *
     * @param image 분석 이미지 바이트
     * @param contentType 이미지 MIME 타입
     * @param width 이미지 너비
     * @param height 이미지 높이
     * @return 정규화 좌표를 포함한 분석 결과
     */
    MapAnalysisResult analyze(
            byte[] image,
            String contentType,
            int width,
            int height
    );
}
