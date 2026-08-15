package com.example.chookjibupadmin.api.visitor.dto;

import com.example.chookjibupadmin.visitor.command.application.dto.UpdateVisitorCountCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateVisitorCountRequest(
        @Schema(description = "방문 인원 수", example = "12000")
        @NotNull
        @PositiveOrZero
        Integer visitorCount
) {

    public UpdateVisitorCountCommand toCommand() {
        return new UpdateVisitorCountCommand(visitorCount);
    }
}
