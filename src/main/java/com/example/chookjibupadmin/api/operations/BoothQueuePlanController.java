package com.example.chookjibupadmin.api.operations;

import com.example.chookjibupadmin.api.operations.dto.SaveQueuePlanRequest;
import com.example.chookjibupadmin.api.operations.dto.RecommendQueuePlanRequest;
import com.example.chookjibupadmin.booth.command.application.QueuePlanRecommendationApplicationService;
import com.example.chookjibupadmin.booth.command.application.QueueRecommendationContextService;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothQueuePlanApplicationService;
import com.example.chookjibupadmin.booth.command.application.dto.QueuePlanView;
import com.example.chookjibupadmin.global.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 승인 부스의 사전 대기 동선 API이다. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Booth Queue Plan", description = "사전 대기 동선 설정")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/festivals/{festivalId}/operations/booths/{boothId}/queue-plan")
public class BoothQueuePlanController {
    private final BoothQueuePlanApplicationService service;
    private final QueuePlanRecommendationApplicationService recommendationService;
    private final QueueRecommendationContextService contextService;
    @GetMapping("/recommendations/status")
    @Operation(summary = "AI 줄 추천 사용 가능 여부", description = "권한을 검증한 후 서버 활성화 상태와 안내 문구를 반환한다. 외부 AI 호출이나 저장은 수행하지 않는다.")
    public ApiResponse<QueuePlanRecommendationApplicationService.Availability> availability(
            @PathVariable UUID festivalId, @PathVariable Long boothId, @AuthenticationPrincipal Object principal) {
        return ApiResponse.success(SuccessCode.FESTIVAL_QUEUE_READ_SUCCESS,
                recommendationService.availability(festivalId, boothId, actor(principal)));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "AI 사전 동선 추천", description = "동기식 추천. 경계와 부스 좌표 필요. 미확정 결과를 PUT으로 저장한다.")
    public ApiResponse<QueuePlanRecommendationApplicationService.Recommendation> recommend(
            @PathVariable UUID festivalId, @PathVariable Long boothId,
            @Valid @RequestBody RecommendQueuePlanRequest request, @AuthenticationPrincipal Object principal) {
        return ApiResponse.success(SuccessCode.FESTIVAL_QUEUE_READ_SUCCESS,
                recommendationService.recommend(festivalId, boothId, request.targetCapacity(),
                        request.metersPerPerson(), actor(principal)));
    }

    @GetMapping("/candidates")
    @Operation(summary = "도면 대기선 후보 조회", description = "기존 AI/수동 QUEUE 선을 가져온다. 후보 적용도 PUT 저장을 사용한다.")
    public ApiResponse<java.util.List<QueueRecommendationContextService.LineCandidate>> candidates(
            @PathVariable UUID festivalId, @PathVariable Long boothId, @AuthenticationPrincipal Object principal) {
        return ApiResponse.success(SuccessCode.FESTIVAL_QUEUE_READ_SUCCESS,
                contextService.candidates(festivalId, boothId, actor(principal)));
    }

    @GetMapping
    @Operation(summary = "사전 대기 동선 조회", description = "동선이 없으면 404. 관리자와 담당 스태프가 조회한다.")
    public ApiResponse<QueuePlanView> get(@PathVariable UUID festivalId, @PathVariable Long boothId,
            @AuthenticationPrincipal Object principal) {
        return ApiResponse.success(SuccessCode.FESTIVAL_QUEUE_READ_SUCCESS,
                service.get(festivalId, boothId, actor(principal)));
    }

    @PutMapping
    @Operation(summary = "사전 대기 동선 저장", description = "축제 정보 수정 권한 필요. 최초 expectedRevision은 0, nodeVersion은 지도 조회값.")
    public ApiResponse<QueuePlanView> save(@PathVariable UUID festivalId, @PathVariable Long boothId,
            @Valid @RequestBody SaveQueuePlanRequest request, @AuthenticationPrincipal Object principal) {
        return ApiResponse.success(SuccessCode.FESTIVAL_QUEUE_UPDATE_SUCCESS,
                service.save(festivalId, boothId, request.toCommand(), actor(principal)));
    }
    private FestivalActorPrincipal actor(Object principal) {
        if (principal instanceof FestivalActorPrincipal actor) return actor;
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
