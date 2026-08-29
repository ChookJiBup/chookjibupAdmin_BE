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

@Schema(
        description = """
                축제 장소·주소 입력.
                대표 장소(primary=true)는 latitude·longitude가 모두 필수이다.
                보조 장소는 둘 다 null이거나 둘 다 있어야 한다.
                좌표가 있으면 대한민국 인근 허용 범위(위도 33.0~38.7, 경도 124.5~132.0)를 검증한다.
                누락 시 40013, 범위 밖이면 40014.
                """
)
public record FestivalLocationRequest(
        @Schema(description = "수정할 기존 장소 UUID. 신규 장소는 생략") UUID locationId,
        @NotNull FestivalLocationType locationType,
        @NotBlank @Size(max = 150) String locationName,
        @Size(max = 255) String roadAddress,
        @Size(max = 255) String jibunAddress,
        @Size(max = 100) String detailAddress,
        @Size(max = 10) String postalCode,
        @Size(max = 30) String buildingManagementNumber,
        @Schema(
                description = "위도. 대표 장소는 필수. 보조는 null 허용(경도와 함께)",
                example = "37.5665",
                nullable = true
        )
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @Schema(
                description = "경도. 대표 장소는 필수. 보조는 null 허용(위도와 함께)",
                example = "126.9780",
                nullable = true
        )
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Schema(description = "주소 한 점으로 표현하기 어려운 운영 권역 GeoJSON")
        Map<String, Object> boundaryGeometry,
        @Schema(description = "대표 장소 여부. 목록에 정확히 하나여야 한다")
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
