package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 축제 방문 인원 집계 방식 변경 응답 DTO이다.
 */
@Schema(description = "축제 방문 인원 집계 방식 변경 응답")
public record FestivalVisitorCountInputModeResponse(
        @Schema(description = "외부 노출용 축제 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,

        @Schema(description = "변경된 방문 인원 집계 방식", example = "DAILY")
        FestivalVisitorCountInputMode visitorCountInputMode
) {

    /**
     * 축제 Aggregate를 집계 방식 응답으로 변환한다.
     */
    public static FestivalVisitorCountInputModeResponse from(Festival festival) {
        return new FestivalVisitorCountInputModeResponse(
                festival.getPublicId(),
                festival.getVisitorCountInputMode()
        );
    }
}
