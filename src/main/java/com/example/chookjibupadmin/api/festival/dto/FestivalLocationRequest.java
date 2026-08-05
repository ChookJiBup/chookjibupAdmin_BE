package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "축제 장소·주소 입력")
public record FestivalLocationRequest(
        @Schema(description = "수정할 기존 장소 UUID. 신규 장소는 생략") UUID locationId,
        @NotNull FestivalLocationType locationType,
        @NotBlank @Size(max = 150) String locationName,
        @Size(max = 255) String roadAddress,
        @Size(max = 255) String jibunAddress,
        @Size(max = 100) String detailAddress,
        @Size(max = 10) String postalCode,
        @Size(max = 30) String buildingManagementNumber,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Schema(description = "주소 한 점으로 표현하기 어려운 운영 권역 GeoJSON")
        Map<String, Object> boundaryGeometry,
        boolean primary,
        @PositiveOrZero int sortOrder
) {
    public FestivalLocationCommand toCommand() {
        return new FestivalLocationCommand(
                locationId,
                locationType,
                locationName,
                roadAddress,
                jibunAddress,
                detailAddress,
                postalCode,
                buildingManagementNumber,
                latitude,
                longitude,
                boundaryGeometry,
                primary,
                sortOrder
        );
    }
}
