package com.example.chookjibupadmin.api.visitor;

import com.example.chookjibupadmin.api.visitor.dto.FestivalDailyVisitorCountResponse;
import com.example.chookjibupadmin.api.visitor.dto.FestivalTotalVisitorCountResponse;
import com.example.chookjibupadmin.api.visitor.dto.FestivalVisitorCountResponse;
import com.example.chookjibupadmin.api.visitor.dto.UpdateVisitorCountRequest;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountApplicationService;
import com.example.chookjibupadmin.visitor.query.application.FestivalVisitorCountQueryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 축제 방문 인원 수 입력·조회 API를 제공한다. 관리자만 호출할 수 있다.
 */
@Tag(name = "Festival Visitor Count", description = "축제 방문 인원 수 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/operations/visitors")
@RequiredArgsConstructor
public class FestivalVisitorCountCommandController {

    private final FestivalVisitorCountApplicationService visitorCountService;
    private final FestivalVisitorCountQueryApplicationService visitorCountQueryService;

    @Operation(summary = "축제 방문 인원 입력 현황 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<FestivalVisitorCountResponse> getVisitorCounts(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_VISITOR_COUNT_READ_SUCCESS,
                FestivalVisitorCountResponse.from(
                        visitorCountQueryService.getVisitorCounts(
                                festivalId,
                                principal
                        )
                )
        );
    }

    @Operation(summary = "축제 일자별 방문 인원 수 입력")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/daily/{visitDate}")
    public ApiResponse<FestivalDailyVisitorCountResponse> updateDaily(
            @PathVariable UUID festivalId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate visitDate,
            @Valid @RequestBody UpdateVisitorCountRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_DAILY_VISITOR_COUNT_UPDATE_SUCCESS,
                FestivalDailyVisitorCountResponse.from(
                        visitorCountService.updateDailyVisitorCount(
                                festivalId,
                                visitDate,
                                request.toCommand(),
                                principal
                        )
                )
        );
    }

    @Operation(summary = "축제 총 방문 인원 수 입력")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/total")
    public ApiResponse<FestivalTotalVisitorCountResponse> updateTotal(
            @PathVariable UUID festivalId,
            @Valid @RequestBody UpdateVisitorCountRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_TOTAL_VISITOR_COUNT_UPDATE_SUCCESS,
                FestivalTotalVisitorCountResponse.from(
                        visitorCountService.updateTotalVisitorCount(
                                festivalId,
                                request.toCommand(),
                                principal
                        )
                )
        );
    }
}
