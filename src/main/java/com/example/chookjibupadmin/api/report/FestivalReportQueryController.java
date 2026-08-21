package com.example.chookjibupadmin.api.report;

import com.example.chookjibupadmin.api.report.dto.FestivalReportEvaluationResponse;
import com.example.chookjibupadmin.api.report.dto.FestivalReportGenerateResponse;
import com.example.chookjibupadmin.api.report.dto.FestivalReportPerformanceResponse;
import com.example.chookjibupadmin.api.report.dto.FestivalReportStatusResponse;
import com.example.chookjibupadmin.api.report.dto.FestivalReportSummaryResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.report.command.application.FestivalReportGenerationApplicationService;
import com.example.chookjibupadmin.report.query.application.FestivalReportDetailQueryApplicationService;
import com.example.chookjibupadmin.report.query.application.FestivalReportQueryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 축제 종료 후 결과 보고서 조회·생성 API를 제공한다.
 */
@Tag(name = "Festival Report", description = "축제 결과 보고서 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/reports")
@RequiredArgsConstructor
public class FestivalReportQueryController {

    private final FestivalReportQueryApplicationService reportQueryService;
    private final FestivalReportDetailQueryApplicationService reportDetailQueryService;
    private final FestivalReportGenerationApplicationService reportGenerationService;

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

    @Operation(summary = "축제 결과 보고서 상태 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/status")
    public ApiResponse<FestivalReportStatusResponse> getStatus(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_REPORT_STATUS_READ_SUCCESS,
                FestivalReportStatusResponse.from(
                        reportDetailQueryService.getStatus(festivalId, principal)
                )
        );
    }

    @Operation(summary = "축제 결과 보고서 생성")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/generate")
    public ApiResponse<FestivalReportGenerateResponse> generate(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_REPORT_GENERATE_SUCCESS,
                FestivalReportGenerateResponse.from(
                        reportGenerationService.generate(festivalId, principal)
                )
        );
    }

    @Operation(summary = "축제 성과 보고서 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/performance")
    public ApiResponse<FestivalReportPerformanceResponse> getPerformance(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_REPORT_PERFORMANCE_READ_SUCCESS,
                FestivalReportPerformanceResponse.from(
                        reportDetailQueryService.getPerformance(
                                festivalId,
                                principal
                        )
                )
        );
    }

    @Operation(summary = "축제 방문객 평가 보고서 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/evaluation")
    public ApiResponse<FestivalReportEvaluationResponse> getEvaluation(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_REPORT_EVALUATION_READ_SUCCESS,
                FestivalReportEvaluationResponse.from(
                        reportDetailQueryService.getEvaluation(
                                festivalId,
                                principal
                        )
                )
        );
    }
}
