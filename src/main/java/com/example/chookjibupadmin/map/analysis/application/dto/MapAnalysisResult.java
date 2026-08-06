package com.example.chookjibupadmin.map.analysis.application.dto;

import java.util.List;

public record MapAnalysisResult(List<AnalyzedMapNode> nodes) {

    public MapAnalysisResult {
        nodes = nodes == null
                ? List.of()
                : List.copyOf(nodes);
    }
}
