package com.example.chookjibupadmin.api.address.dto;

import com.example.chookjibupadmin.address.query.application.dto.RoadAddressView;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 도로명주소 검색 결과 한 건의 API 응답이다.
 */
@Schema(description = "도로명주소 검색 결과")
public record RoadAddressResponse(
        @Schema(description = "참고항목을 포함한 전체 도로명주소")
        String roadAddress,
        @Schema(description = "참고항목을 제외한 도로명주소")
        String roadAddressPart1,
        @Schema(description = "법정동과 공동주택명 등 참고항목")
        String roadAddressPart2,
        @Schema(description = "지번주소")
        String jibunAddress,
        @Schema(description = "우편번호")
        String zipCode,
        @Schema(description = "건물명")
        String buildingName,
        @Schema(description = "건물관리번호")
        String buildingManagementNumber
) {

    /**
     * application 조회 결과를 API 응답으로 변환한다.
     */
    public static RoadAddressResponse from(RoadAddressView view) {
        return new RoadAddressResponse(
                view.roadAddress(),
                view.roadAddressPart1(),
                view.roadAddressPart2(),
                view.jibunAddress(),
                view.zipCode(),
                view.buildingName(),
                view.buildingManagementNumber()
        );
    }
}
