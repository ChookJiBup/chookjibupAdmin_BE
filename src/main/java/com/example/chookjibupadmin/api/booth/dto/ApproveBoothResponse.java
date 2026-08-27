package com.example.chookjibupadmin.api.booth.dto;

import com.example.chookjibupadmin.booth.command.application.dto.ApproveBoothResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "지도 노드 부스 승인 응답")
public record ApproveBoothResponse(
        Long boothId,
        UUID nodeId,
        String boothName
) {
    public static ApproveBoothResponse from(ApproveBoothResult result) {
        return new ApproveBoothResponse(
                result.boothId(),
                result.nodeId(),
                result.boothName()
        );
    }
}
