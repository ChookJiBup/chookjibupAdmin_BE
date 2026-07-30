package com.example.demoadmin.api.booth;

import com.example.demoadmin.api.booth.dto.BoothQueueLineResponse;
import com.example.demoadmin.api.booth.dto.BoothResponse;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.query.application.BoothQueryApplicationService;
import com.example.demoadmin.global.response.ApiResponse;
import com.example.demoadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Booth", description = "축제 부스 및 대기 라인 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/booths")
@RequiredArgsConstructor
public class BoothQueryController {

    private final BoothQueryApplicationService boothQueryApplicationService;

    @Operation(summary = "축제 부스 전체 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<BoothResponse>> getBooths(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_READ_SUCCESS,
                boothQueryApplicationService.getBooths(festivalId, principal)
                        .stream()
                        .map(BoothResponse::from)
                        .toList()
        );
    }

    @Operation(summary = "축제 부스 단건 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{boothId}")
    public ApiResponse<BoothResponse> getBooth(
            @PathVariable UUID festivalId,
            @PathVariable UUID boothId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_READ_SUCCESS,
                BoothResponse.from(boothQueryApplicationService.getBooth(
                        festivalId,
                        boothId,
                        principal
                ))
        );
    }

    @Operation(summary = "부스 대기 라인 전체 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{boothId}/queue-lines")
    public ApiResponse<List<BoothQueueLineResponse>> getQueueLines(
            @PathVariable UUID festivalId,
            @PathVariable UUID boothId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_READ_SUCCESS,
                boothQueryApplicationService.getQueueLines(
                                festivalId,
                                boothId,
                                principal
                        )
                        .stream()
                        .map(BoothQueueLineResponse::from)
                        .toList()
        );
    }
}
