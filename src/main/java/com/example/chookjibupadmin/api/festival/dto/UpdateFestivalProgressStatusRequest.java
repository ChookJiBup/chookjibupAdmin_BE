package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 자동 모드와 수동 상태를 명시적으로 구분하는 요청이다. */
public record UpdateFestivalProgressStatusRequest(
        @NotNull @Schema(description = "true: 날짜 자동, false: 직접 지정") Boolean automatic,
        @Schema(description = "자동 모드에서는 생략, 수동 모드에서는 필수") FestivalProgressStatus progressStatus
) {
    public FestivalProgressStatus toOverride() {
        if (automatic == null || (automatic && progressStatus != null)
                || (!automatic && progressStatus == null)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return progressStatus;
    }
}
