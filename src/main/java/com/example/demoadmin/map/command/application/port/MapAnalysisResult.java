package com.example.demoadmin.map.command.application.port;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.List;

public record MapAnalysisResult(
        List<DetectedMapObject> objects
) {

    public MapAnalysisResult {
        if (objects == null || objects.stream().anyMatch(java.util.Objects::isNull)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        objects = List.copyOf(objects);
    }
}
