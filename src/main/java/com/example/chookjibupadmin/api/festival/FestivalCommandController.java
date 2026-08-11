package com.example.chookjibupadmin.api.festival;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.CreateFestivalResponse;
import com.example.chookjibupadmin.api.festival.dto.CreateFestivalWithMapResponse;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalDeleteApplicationService;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationQueryApplicationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapRegistrationApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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
    private final FestivalMapRegistrationApplicationService
            festivalMapRegistrationApplicationService;
    private final FestivalLocationQueryApplicationService locationQueryService;

    /**
     * 임시 기준의 축제 기본 정보를 저장한다.
     */
    @Operation(summary = "축제 기본 정보 생성")
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
                                principal
                        )
                )
        );
    }

    /**
     * 축제 기본 정보와 AI 분석 대상 원본 도면을 함께 등록한다.
     */
    @Operation(summary = "AI 분석용 원본 도면을 포함한 축제 기본 정보 생성")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreateFestivalWithMapResponse> createWithMap(
            @Valid @RequestPart("festival") CreateFestivalRequest request,
            @RequestPart("image") MultipartFile blueprintImage,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        CreateFestivalWithMapResult result = festivalMapRegistrationApplicationService.create(
                request.toCommand(),
                new MapImageUploadCommand(
                        blueprintImage.getOriginalFilename(),
                        blueprintImage.getContentType(),
                        blueprintImage.getSize(),
                        blueprintImage::getInputStream
                ),
                principal
        );
        return ApiResponse.success(
                SuccessCode.FESTIVAL_CREATE_SUCCESS,
                CreateFestivalWithMapResponse.from(
                        result,
                        locationQueryService.getLocations(
                                result.festival().getPublicId(),
                                principal
                        )
                )
        );
    }

    /**
     * 1관리자 권한으로 담당 축제의 기본 정보를 수정한다.
     */
    @Operation(summary = "축제 기본 정보 수정")
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
