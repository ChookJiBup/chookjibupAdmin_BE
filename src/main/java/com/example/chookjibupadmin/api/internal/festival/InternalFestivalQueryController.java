package com.example.chookjibupadmin.api.internal.festival;

import com.example.chookjibupadmin.api.festival.dto.FestivalLocationResponse;
import com.example.chookjibupadmin.api.internal.festival.dto.InternalFestivalPageResponse;
import com.example.chookjibupadmin.festival.location.application.InternalFestivalLocationQueryApplicationService;
import com.example.chookjibupadmin.festival.query.application.InternalFestivalQueryApplicationService;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 서버가 호출하는 internal 축제 조회 API를 제공한다.
 */
@Tag(name = "Internal Festival", description = "서버 간 축제 조회 API")
@RestController
@RequestMapping("/internal/api/festivals")
@RequiredArgsConstructor
public class InternalFestivalQueryController {

    private final InternalFestivalQueryApplicationService queryService;
    private final InternalFestivalLocationQueryApplicationService locationQueryService;

    @Operation(summary = "사용자 서버용 축제 전체 장소 조회")
    @GetMapping("/{festivalId}/locations")
    public ApiResponse<List<FestivalLocationResponse>> getLocations(@PathVariable UUID festivalId) {
        return ApiResponse.success(
                SuccessCode.INTERNAL_FESTIVAL_LOCATION_READ_SUCCESS,
                locationQueryService.getLocations(festivalId).stream()
                        .map(FestivalLocationResponse::from)
                        .toList()
        );
    }

    /**
     * 사용자 서버에서 사용할 축제 목록을 진행 상태별로 조회한다.
     */
    @Operation(summary = "사용자 서버용 축제 목록 조회")
    @GetMapping
    public ApiResponse<InternalFestivalPageResponse> getFestivals(
            @RequestParam(required = false)
            FestivalProgressStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                SuccessCode.INTERNAL_FESTIVAL_READ_SUCCESS,
                InternalFestivalPageResponse.from(queryService.searchFestivals(
                        status,
                        keyword,
                        page,
                        size
                ))
        );
    }
}
