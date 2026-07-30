package com.example.demoadmin.map.command.application.dto;

import com.example.demoadmin.map.command.domain.MapAnalysisStatus;
import java.util.UUID;

public record MapAnalysisResultView(
        UUID mapId,
        UUID analysisJobId,
        MapAnalysisStatus status,
        int objectCount
) {
}
