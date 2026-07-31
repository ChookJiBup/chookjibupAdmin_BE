package com.example.chookjibupadmin.api.address.dto;

import com.example.chookjibupadmin.address.query.application.dto.RoadAddressSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 도로명주소 검색 페이징 API 응답이다.
 */
@Schema(description = "도로명주소 검색 응답")
public record RoadAddressSearchResponse(
        @Schema(description = "현재 페이지", example = "1")
        int page,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 검색 결과 수", example = "27")
        int totalCount,
        List<RoadAddressResponse> addresses
) {

    /**
     * application 검색 결과를 API 응답으로 변환한다.
     */
    public static RoadAddressSearchResponse from(
            RoadAddressSearchResult result
    ) {
        return new RoadAddressSearchResponse(
                result.page(),
                result.size(),
                result.totalCount(),
                result.addresses().stream()
                        .map(RoadAddressResponse::from)
                        .toList()
        );
    }
}
