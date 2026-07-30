package com.example.demoadmin.map.command.application.port;

import com.example.demoadmin.map.command.domain.MapStorageType;
import java.util.UUID;

public record MapImageAnalysisRequest(
        UUID mapId,
        String storagePath,
        MapStorageType storageType,
        int width,
        int height
) {
}
