package com.example.demoadmin.map.command.application.dto;

import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import java.util.UUID;

public record PreparedMapAnalysis(
        Long festivalMapId,
        UUID festivalMapPublicId,
        Long analysisJobId,
        UUID analysisJobPublicId,
        MapImageAnalysisRequest analysisRequest
) {
}
