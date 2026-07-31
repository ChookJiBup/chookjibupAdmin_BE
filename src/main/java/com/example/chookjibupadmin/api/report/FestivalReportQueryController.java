package com.example.chookjibupadmin.api.report;

import com.example.chookjibupadmin.api.report.dto.FestivalReportSummaryResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.report.query.application.FestivalReportQueryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 축제 종료 후 결과 보고서 조회 API를 제공한다.
 */
@Tag(name = "Festival Report", description = "축제 결과 보고서 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/reports")
@RequiredArgsConstructor
public class FestivalReportQueryController {

    private final FestivalReportQueryApplicationService reportQueryService;

    /**
     * 담당 축제의 결과 보고서 요약 정보를 조회한다.
     */
    @Operation(summary = "축제 결과 보고서 요약 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/summary")
    public ApiResponse<FestivalReportSummaryResponse> getSummary(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_REPORT_SUMMARY_READ_SUCCESS,
                FestivalReportSummaryResponse.from(
                        reportQueryService.getSummary(festivalId, principal)
                )
        );
    }
}
