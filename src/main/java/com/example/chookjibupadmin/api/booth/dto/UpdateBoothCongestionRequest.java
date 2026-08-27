package com.example.chookjibupadmin.api.booth.dto;

import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothCongestionCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부스 혼잡 입력 요청")
public record UpdateBoothCongestionRequest(
        @NotNull
        @Min(0)
        Integer waitMinutes,
        @NotNull
        BoothCongestionLevel congestionLevel
) {
    public UpdateBoothCongestionCommand toCommand() {
        return new UpdateBoothCongestionCommand(waitMinutes, congestionLevel);
    }
}
