package com.example.chookjibupadmin.api.map;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalMapResponse;
import com.example.chookjibupadmin.api.map.dto.CreateCoordinateMapRequest;
import com.example.chookjibupadmin.api.map.dto.CreateCoordinateMapResponse;
import com.example.chookjibupadmin.api.map.dto.MapImageAnchorResponse;
import com.example.chookjibupadmin.api.map.dto.PublishRoadmapResponse;
import com.example.chookjibupadmin.api.map.dto.SaveRoadmapDraftRequest;
import com.example.chookjibupadmin.api.map.dto.SaveRoadmapDraftResponse;
import com.example.chookjibupadmin.api.map.dto.UpdateMapImageAnchorRequest;
import com.example.chookjibupadmin.api.map.dto.UploadMapOverlayResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapCoordinateRegistrationApplicationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapManagementApplicationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapOverlayUploadApplicationService;
import com.example.chookjibupadmin.map.command.application.RoadmapDraftApplicationService;
import com.example.chookjibupadmin.map.command.application.RoadmapPublishApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 분석용 축제 도면의 교체·삭제와 배치도 이미지 앵커·오버레이 수정 API를 제공한다.
 */
@Tag(name = "Festival Map", description = "축제 배치도 관리 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/maps")
@RequiredArgsConstructor
public class FestivalMapCommandController {

    private final FestivalMapManagementApplicationService managementService;
    private final FestivalMapCoordinateRegistrationApplicationService coordinateRegistrationService;
    private final FestivalMapOverlayUploadApplicationService overlayUploadService;
    private final RoadmapDraftApplicationService roadmapDraftService;
    private final RoadmapPublishApplicationService roadmapPublishService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "좌표 전용 축제 지도 준비")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ApiResponse<CreateCoordinateMapResponse> createCoordinateMap(
            @PathVariable UUID festivalId,
            @Valid @RequestBody CreateCoordinateMapRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_CREATE_SUCCESS,
                CreateCoordinateMapResponse.from(coordinateRegistrationService.ensureCoordinateMap(
                        festivalId,
                        request.mapName(),
                        principal
                ))
        );
    }

    @Operation(summary = "축제 지도 노드 편집 내용 일괄 저장")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{mapId}/editor")
    public ApiResponse<SaveRoadmapDraftResponse> saveEditor(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @Valid @RequestBody SaveRoadmapDraftRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_EDITOR_SAVE_SUCCESS,
                SaveRoadmapDraftResponse.from(roadmapDraftService.save(
                        festivalId,
                        mapId,
                        request.toCommand(objectMapper),
                        principal
                ))
        );
    }

    @Operation(summary = "축제 부스맵 방문객 공개")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{mapId}/publish")
    public ApiResponse<PublishRoadmapResponse> publish(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_PUBLISH_SUCCESS,
                PublishRoadmapResponse.from(roadmapPublishService.publish(
                        festivalId,
                        mapId,
                        principal
                ))
        );
    }

    @Operation(summary = "축제 부스맵 방문객 공개 해제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{mapId}/publish")
    public ApiResponse<PublishRoadmapResponse> unpublish(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_UNPUBLISH_SUCCESS,
                PublishRoadmapResponse.from(roadmapPublishService.unpublish(
                        festivalId,
                        mapId,
                        principal
                ))
        );
    }

    @Operation(summary = "축제 배치도 이미지 앵커 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{mapId}/image-anchor")
    public ApiResponse<MapImageAnchorResponse> updateImageAnchor(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @Valid @RequestBody UpdateMapImageAnchorRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_ANCHOR_UPDATE_SUCCESS,
                MapImageAnchorResponse.of(mapId, managementService.updateImageAnchor(
                        festivalId,
                        mapId,
                        request.centerLat(),
                        request.centerLng(),
                        request.groundWidthMeters(),
                        request.rotationDegrees(),
                        principal
                ))
        );
    }

    @Operation(summary = "축제 지도 오버레이 이미지 등록")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/{mapId}/overlay",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UploadMapOverlayResponse> uploadOverlay(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @RequestPart("image") MultipartFile overlayImage,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_OVERLAY_UPLOAD_SUCCESS,
                UploadMapOverlayResponse.from(overlayUploadService.upload(
                        festivalId,
                        mapId,
                        new MapImageUploadCommand(
                                overlayImage.getOriginalFilename(),
                                overlayImage.getContentType(),
                                overlayImage.getSize(),
                                overlayImage::getInputStream
                        ),
                        principal
                ))
        );
    }

    @Operation(summary = "AI 분석용 축제 도면 이미지 교체")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/{mapId}/replacement",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<CreateFestivalMapResponse> replace(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @RequestPart(value = "mapName", required = false) String mapName,
            @RequestPart("image") MultipartFile blueprintImage,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_MAP_REPLACE_SUCCESS,
                CreateFestivalMapResponse.from(managementService.replace(
                        festivalId,
                        mapId,
                        mapName,
                        new MapImageUploadCommand(
                                blueprintImage.getOriginalFilename(),
                                blueprintImage.getContentType(),
                                blueprintImage.getSize(),
                                blueprintImage::getInputStream
                        ),
                        principal
                ))
        );
    }

    @Operation(summary = "축제 도면 이미지 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{mapId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        managementService.delete(festivalId, mapId, principal);
        return ApiResponse.success(SuccessCode.FESTIVAL_MAP_DELETE_SUCCESS);
    }
}
