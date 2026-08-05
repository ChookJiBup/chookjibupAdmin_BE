package com.example.chookjibupadmin.api.festival;

import com.example.chookjibupadmin.api.festival.dto.FestivalLocationResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationQueryApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals/{festivalId}/locations")
@RequiredArgsConstructor
public class FestivalLocationQueryController {

    private final FestivalLocationQueryApplicationService service;

    @Operation(summary = "축제 전체 장소·주소 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<FestivalLocationResponse>> getLocations(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_LOCATION_READ_SUCCESS,
                service.getLocations(festivalId, principal).stream()
                        .map(FestivalLocationResponse::from)
                        .toList()
        );
    }
}
