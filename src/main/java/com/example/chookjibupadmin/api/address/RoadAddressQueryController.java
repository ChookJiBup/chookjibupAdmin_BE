package com.example.chookjibupadmin.api.address;

import com.example.chookjibupadmin.address.query.application.RoadAddressSearchService;
import com.example.chookjibupadmin.api.address.dto.RoadAddressSearchResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 화면의 도로명주소 검색 API를 제공한다.
 */
@Tag(name = "Road Address", description = "공식 도로명주소 검색 API")
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class RoadAddressQueryController {

    private final RoadAddressSearchService roadAddressSearchService;

    /**
     * 도로명 또는 건물명 검색어로 공식 도로명주소를 조회한다.
     */
    @Operation(summary = "도로명주소 검색")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/search")
    public ApiResponse<RoadAddressSearchResponse> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ROAD_ADDRESS_SEARCH_SUCCESS,
                RoadAddressSearchResponse.from(
                        roadAddressSearchService.search(
                                keyword,
                                page,
                                size,
                                principal
                        )
                )
        );
    }
}
