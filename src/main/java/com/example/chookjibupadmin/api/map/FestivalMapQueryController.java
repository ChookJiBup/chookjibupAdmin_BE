package com.example.chookjibupadmin.api.map;

import com.example.chookjibupadmin.api.map.dto.FestivalMapReadUrlResponse;
import com.example.chookjibupadmin.api.map.dto.MapAnalysisStatusResponse;
import com.example.chookjibupadmin.api.map.dto.MapEditorResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapReadUrlApplicationService;
import com.example.chookjibupadmin.map.query.application.FestivalMapAnalysisQueryApplicationService;
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

@Tag(name = "Festival Map", description = "축제 배치도 관리 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/maps")
@RequiredArgsConstructor
public class FestivalMapQueryController {

    private final FestivalMapReadUrlApplicationService readUrlService;
    private final FestivalMapAnalysisQueryApplicationService analysisQueryService;

    @Operation(summary = "축제 도면 OpenAI 분석 상태 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{mapId}/analysis")
    public ApiResponse<MapAnalysisStatusResponse> analysisStatus(@PathVariable UUID festivalId,
            @PathVariable UUID mapId,@AuthenticationPrincipal AdminPrincipal principal){
        return ApiResponse.success(SuccessCode.FESTIVAL_MAP_ANALYSIS_READ_SUCCESS,
                MapAnalysisStatusResponse.from(analysisQueryService.status(festivalId,mapId,principal)));
    }

    @Operation(summary = "축제 지도 편집용 배경 이미지와 분석 노드 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{mapId}/editor")
    public ApiResponse<MapEditorResponse> editor(@PathVariable UUID festivalId,@PathVariable UUID mapId,
            @AuthenticationPrincipal AdminPrincipal principal){
        return ApiResponse.success(SuccessCode.FESTIVAL_MAP_EDITOR_READ_SUCCESS,
                MapEditorResponse.from(analysisQueryService.editor(festivalId,mapId,principal)));
    }

    @Operation(summary = "현재 축제 도면 검수 화면용 조회 URL 발급")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{mapId}/read-url")
    public ApiResponse<FestivalMapReadUrlResponse> createReadUrl(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_READ_URL_SUCCESS,
                FestivalMapReadUrlResponse.from(
                        readUrlService.createReadUrl(
                                festivalId,
                                mapId,
                                principal
                        )
                )
        );
    }
}
