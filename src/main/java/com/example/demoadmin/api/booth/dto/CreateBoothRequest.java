package com.example.demoadmin.api.booth.dto;

import com.example.demoadmin.booth.command.application.dto.CreateBoothCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "축제 부스 생성 요청")
public record CreateBoothRequest(
        @Schema(description = "부스 이름", example = "김밥 A")
        @NotBlank
        String name,

        @Schema(description = "부스 카테고리", example = "FOOD")
        @NotBlank
        String category,

        @Schema(description = "부스 위치", example = "A구역 1번")
        @NotBlank
        String location,

        @Schema(description = "부스 설명", example = "지역 김밥 판매 부스")
        String description
) {

    public CreateBoothCommand toCommand() {
        return new CreateBoothCommand(
                name,
                category,
                location,
                description
        );
    }
}
