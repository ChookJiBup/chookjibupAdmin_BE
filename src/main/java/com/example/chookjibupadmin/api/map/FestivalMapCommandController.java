package com.example.chookjibupadmin.api.map;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalMapResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapManagementApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 분석용 축제 도면의 교체와 삭제 API를 제공한다.
 */
@Tag(name = "Festival Map", description = "축제 배치도 관리 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/maps")
@RequiredArgsConstructor
public class FestivalMapCommandController {

    private final FestivalMapManagementApplicationService managementService;

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
