package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;

@Schema(description = "축제 도면 관리자 검수 화면용 임시 조회 URL 응답")
public record FestivalMapReadUrlResponse(
        @Schema(description = "S3 Presigned GET URL")
        URI readUrl,
        @Schema(description = "조회 URL 만료 시각")
        Instant expiresAt
) {

    public static FestivalMapReadUrlResponse from(MapImageReadUrl readUrl) {
        return new FestivalMapReadUrlResponse(
                readUrl.url(),
                readUrl.expiresAt()
        );
    }
}
