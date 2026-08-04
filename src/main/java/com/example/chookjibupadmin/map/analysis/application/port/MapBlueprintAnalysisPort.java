package com.example.chookjibupadmin.map.analysis.application.port;
import com.example.chookjibupadmin.map.analysis.application.dto.MapAnalysisResult;
public interface MapBlueprintAnalysisPort {
    MapAnalysisResult analyze(byte[] image, String contentType, int width, int height);
}
