package com.example.demoadmin.api.booth.dto;

import com.example.demoadmin.booth.command.domain.BoothOperatingStatus;
import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.application.dto.BoothQueueTailResult;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "축제 부스 응답")
public record BoothResponse(
        @Schema(description = "부스 ID")
        UUID boothId,
        @Schema(description = "부스 이름")
        String name,
        @Schema(description = "부스 카테고리")
        String category,
        @Schema(description = "부스 위치")
        String location,
        @Schema(description = "부스 설명")
        String description,
        @Schema(description = "운영 상태")
        BoothOperatingStatus operatingStatus,
        @Schema(description = "현재 줄 끝 대기 라인 ID")
        UUID currentQueueLineId,
        @Schema(description = "현재 줄 끝 대기 라인 순서")
        Integer currentQueueLineOrder,
        @Schema(description = "현재 줄 끝 대기 라인 표시명")
        String currentQueueLineLabel,
        @Schema(description = "예상 대기시간 분")
        int expectedWaitingMinutes
) {

    public static BoothResponse from(FestivalBooth booth) {
        return new BoothResponse(
                booth.getPublicId(),
                booth.getNameValue(),
                booth.getCategoryValue(),
                booth.getLocationValue(),
                booth.getDescriptionValue(),
                booth.getOperatingStatus(),
                null,
                null,
                null,
                booth.getExpectedWaitingMinutes()
        );
    }

    public static BoothResponse from(BoothView view) {
        return new BoothResponse(
                view.boothId(),
                view.name(),
                view.category(),
                view.location(),
                view.description(),
                view.operatingStatus(),
                view.currentQueueLineId(),
                view.currentQueueLineOrder(),
                view.currentQueueLineLabel(),
                view.expectedWaitingMinutes()
        );
    }

    public static BoothResponse from(BoothQueueTailResult result) {
        FestivalBooth booth = result.booth();
        if (result.currentQueueLine() == null) {
            return from(booth);
        }

        return new BoothResponse(
                booth.getPublicId(),
                booth.getNameValue(),
                booth.getCategoryValue(),
                booth.getLocationValue(),
                booth.getDescriptionValue(),
                booth.getOperatingStatus(),
                result.currentQueueLine().getPublicId(),
                result.currentQueueLine().getLineOrder(),
                result.currentQueueLine().getLabelValue(),
                booth.getExpectedWaitingMinutes()
        );
    }
}
