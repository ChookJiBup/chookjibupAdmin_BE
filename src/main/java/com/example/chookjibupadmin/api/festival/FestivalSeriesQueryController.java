package com.example.chookjibupadmin.api.festival;

import com.example.chookjibupadmin.api.festival.dto.FestivalSeriesSearchResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.query.application.FestivalSeriesQueryApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 축제 등록용 기존 축제 검색 API를 제공한다.
 */
@Tag(name = "Festival Series", description = "축제 등록용 기존 축제 검색 API")
@RestController
@RequestMapping("/api/festival-series")
@RequiredArgsConstructor
public class FestivalSeriesQueryController {

    private final FestivalSeriesQueryApplicationService queryApplicationService;

    /**
     * 축제명 일부로 기존 축제 시리즈와 최근 개최 정보를 검색한다.
     */
    @Operation(summary = "기존 축제 검색")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/search")
    public ApiResponse<List<FestivalSeriesSearchResponse>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_SERIES_SEARCH_SUCCESS,
                queryApplicationService.search(keyword, limit, principal)
                        .stream()
                        .map(FestivalSeriesSearchResponse::from)
                        .toList()
        );
    }
}
