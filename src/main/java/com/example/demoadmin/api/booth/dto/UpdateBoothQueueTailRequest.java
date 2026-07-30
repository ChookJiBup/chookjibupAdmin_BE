package com.example.demoadmin.api.booth.dto;

import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueTailCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부스 줄 끝 갱신 요청")
public record UpdateBoothQueueTailRequest(
        @Schema(description = "현재 줄 끝 대기 라인 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID queueLineId,

        @Schema(description = "부스 운영 상태. 미입력 시 OPERATING", example = "OPERATING")
        String status
) {

    public UpdateBoothQueueTailCommand toCommand() {
        return new UpdateBoothQueueTailCommand(
                queueLineId,
                status
        );
    }
}
