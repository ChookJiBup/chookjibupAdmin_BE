package com.example.chookjibupadmin.map.command.application.dto;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * 오버레이 이미지 업로드 결과. 표시 설정(opacity/visible/clip)은 포함하지 않는다.
 * FE가 로컬 편집값을 유지한 채 asset만 병합하도록 자산 메타만 반환한다.
 */
public record UploadedMapOverlay(
        UUID assetId,
        URI imageUrl,
        Instant imageUrlExpiresAt,
        int imageWidth,
        int imageHeight
) {
}
