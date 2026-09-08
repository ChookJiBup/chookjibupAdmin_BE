package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.UploadedMapOverlay;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record UploadMapOverlayResponse(
        UUID assetId,
        URI imageUrl,
        Instant imageUrlExpiresAt,
        int imageWidth,
        int imageHeight
) {
    public static UploadMapOverlayResponse from(UploadedMapOverlay overlay) {
        return new UploadMapOverlayResponse(
                overlay.assetId(),
                overlay.imageUrl(),
                overlay.imageUrlExpiresAt(),
                overlay.imageWidth(),
                overlay.imageHeight()
        );
    }
}
