package com.example.demoadmin.api.map;

import com.example.demoadmin.api.map.dto.MapAnalysisResponse;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.global.response.ApiResponse;
import com.example.demoadmin.global.response.SuccessCode;
import com.example.demoadmin.map.command.application.MapAnalysisApplicationService;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "축제 배치도 분석 및 객체 조회 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/maps")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.map.analysis.test-endpoint-enabled",
        havingValue = "true"
)
public class MapCommandController {

    private final MapAnalysisApplicationService mapAnalysisApplicationService;

    @Operation(
            summary = "고정 테스트 배치도 분석",
            description = "김밥축제 테스트 이미지로 분석 저장 흐름을 실행합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/test-analysis")
    public ApiResponse<MapAnalysisResponse> analyzeTestMap(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.MAP_ANALYSIS_SUCCESS,
                MapAnalysisResponse.from(
                        mapAnalysisApplicationService.analyzeTestMap(
                                festivalId,
                                CreateTestMapAnalysisCommand.testFixture(),
                                principal
                        )
                )
        );
    }
}
