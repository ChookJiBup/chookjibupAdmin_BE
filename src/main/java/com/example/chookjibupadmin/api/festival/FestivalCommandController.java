package com.example.chookjibupadmin.api.festival;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.CreateFestivalResponse;
import com.example.chookjibupadmin.api.festival.dto.FestivalVisitorCountInputModeResponse;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalProgressStatusRequest;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalResponse;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalVisitorCountInputModeRequest;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalDeleteApplicationService;
import com.example.chookjibupadmin.festival.command.application.FestivalImageRegistrationApplicationService;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationQueryApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.chookjibupadmin.festival.support.ReviewQrUrlBuilder;

/**
 * 축제 기본 정보 쓰기 API를 제공한다.
 */
@Tag(name = "Festival", description = "축제 기본 정보 API")
@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
public class FestivalCommandController {

    private final FestivalApplicationService festivalApplicationService;
    private final FestivalDeleteApplicationService festivalDeleteApplicationService;
    private final FestivalImageRegistrationApplicationService imageRegistrationService;
    private final FestivalLocationQueryApplicationService locationQueryService;
    private final ReviewQrUrlBuilder reviewQrUrlBuilder;

    /**
     * 임시 기준의 축제 기본 정보를 저장한다.
     */
    @Operation(
            summary = "축제 기본 정보 생성",
            description = "대표 장소(primary) 위경도 필수. 누락 40013, 대한민국 인근 범위 밖 40014."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CreateFestivalResponse> create(
            @Valid @RequestBody CreateFestivalRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        Festival festival = festivalApplicationService.create(request.toCommand(), principal);
        return ApiResponse.success(
                SuccessCode.FESTIVAL_CREATE_SUCCESS,
                CreateFestivalResponse.from(
                        festival,
                        locationQueryService.getLocations(
                                festival.getPublicId(),
                                principal)
                        ,reviewQrUrlBuilder.buildReviewUrl(festival.getPublicId())
                )
        );
    }

    /**
     * 축제 기본 정보와 사용자 화면 대표 썸네일을 함께 등록한다.
     */
    @Operation(
            summary = "대표 썸네일을 포함한 축제 기본 정보 생성",
            description = "대표 썸네일은 지도·AI 분석에 사용하지 않는다. "
                    + "대표 장소 위경도 필수(JSON 파트와 동일). 누락 40013, 범위 밖 40014."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreateFestivalResponse> createWithImage(
            @Valid @RequestPart("festival") CreateFestivalRequest request,
            @RequestPart("image") MultipartFile representativeImage,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        Festival festival = imageRegistrationService.create(
                request.toCommand(),
                new FestivalThumbnailUploadCommand(
                        representativeImage.getOriginalFilename(),
                        representativeImage.getContentType(),
                        representativeImage.getSize(),
                        representativeImage::getInputStream
                ),
                principal
        );
        return ApiResponse.success(
                SuccessCode.FESTIVAL_CREATE_SUCCESS,
                CreateFestivalResponse.from(
                        festival,
                        locationQueryService.getLocations(
                                festival.getPublicId(),
                                principal
                        ),
                        reviewQrUrlBuilder.buildReviewUrl(festival.getPublicId())
                )
        );
    }

    /**
     * 1관리자 권한으로 담당 축제의 기본 정보를 수정한다.
     */
    @Operation(
            summary = "축제 기본 정보 수정",
            description = "locations는 필수이며 대표 장소 위경도도 필수. 누락 40013, 범위 밖 40014."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{festivalId}")
    public ApiResponse<UpdateFestivalResponse> update(
            @PathVariable UUID festivalId,
            @Valid @RequestBody UpdateFestivalRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        Festival festival = festivalApplicationService.update(
                festivalId,
                request.toCommand(),
                principal
        );
        return ApiResponse.success(
                SuccessCode.FESTIVAL_UPDATE_SUCCESS,
                UpdateFestivalResponse.from(
                        festival,
                        locationQueryService.getLocations(
                                festivalId,
                                principal
                        )
                )
        );
    }

    /**
     * 1관리자 권한으로 방문 인원 집계 방식만 변경한다.
     */
    @Operation(
            summary = "축제 방문 인원 집계 방식 변경",
            description = "장소 등 다른 기본 정보는 건드리지 않는다. "
                    + "방문 인원 데이터가 이미 있으면 40917."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{festivalId}/visitor-count-input-mode")
    public ApiResponse<FestivalVisitorCountInputModeResponse> updateVisitorCountInputMode(
            @PathVariable UUID festivalId,
            @Valid @RequestBody UpdateFestivalVisitorCountInputModeRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_VISITOR_COUNT_INPUT_MODE_UPDATE_SUCCESS,
                FestivalVisitorCountInputModeResponse.from(
                        festivalApplicationService.changeVisitorCountInputMode(
                                festivalId,
                                request.visitorCountInputMode(),
                                principal
                        )
                )
        );
    }

    @Operation(summary = "축제 진행 상태 변경", description = "총괄관리자 전용. automatic=true로 날짜 자동 모드 복귀.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{festivalId}/progress-status")
    public ApiResponse<Void> updateProgressStatus(
            @PathVariable UUID festivalId,
            @Valid @RequestBody UpdateFestivalProgressStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal principal) {
        festivalApplicationService.changeProgressStatus(festivalId, request.toOverride(), principal);
        return ApiResponse.success(SuccessCode.FESTIVAL_UPDATE_SUCCESS);
    }

    /**
     * 1관리자 권한으로 축제와 모든 관리자용 연관 데이터를 삭제한다.
     */
    @Operation(summary = "축제 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{festivalId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        festivalDeleteApplicationService.delete(festivalId, principal);
        return ApiResponse.success(SuccessCode.FESTIVAL_DELETE_SUCCESS);
    }
}
