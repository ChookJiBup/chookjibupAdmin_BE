package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.dto.UploadedMapOverlay;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카카오맵 표시용 오버레이 이미지 업로드를 조정한다. 지도 교체·분석 재시작과는 분리된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalMapOverlayUploadApplicationService {

    private static final String ROOT_PREFIX = "private/festivals";

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;
    private final FestivalLocationService festivalLocationService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;
    private final FestivalMapPresentationService presentationService;
    private final MapImagePreparationPort mapImagePreparationPort;
    private final MapImageStoragePort mapImageStoragePort;

    public UploadedMapOverlay upload(
            UUID festivalPublicId,
            UUID mapPublicId,
            MapImageUploadCommand imageCommand,
            AdminPrincipal principal
    ) {
        FestivalMap map = authorize(festivalPublicId, mapPublicId, principal);
        UUID assetId = UUID.randomUUID();

        try (PreparedMapImage prepared = mapImagePreparationPort.prepare(imageCommand)) {
            String objectKey = objectKey(
                    festivalPublicId,
                    mapPublicId,
                    assetId,
                    prepared.displayExtension()
            );
            boolean uploadAttempted = false;
            try {
                uploadAttempted = true;
                mapImageStoragePort.upload(new StoredMapImageFile(
                        objectKey,
                        prepared.displayPath(),
                        prepared.displayFileSize(),
                        prepared.displayContentType(),
                        prepared.displayChecksumSha256()
                ));
                var replaced = presentationService.replaceOverlayImage(
                        map.getId(),
                        map.getFestivalId(),
                        MapImageObjectKey.of(objectKey),
                        assetId,
                        prepared.displayImageWidth(),
                        prepared.displayImageHeight(),
                        resolveFallbackAnchor(map)
                );
                FestivalMapPresentation saved = replaced.presentation();
                // 교체가 끝났으니 이전 이미지는 스토리지에서도 지운다. 실패해도 교체는 유효하다.
                deleteReplacedImage(replaced.previousImageKey(), objectKey);
                MapImageReadUrl readUrl = mapImageStoragePort.createReadUrl(objectKey);
                return new UploadedMapOverlay(
                        saved.getOverlayAssetId(),
                        readUrl.url(),
                        readUrl.expiresAt(),
                        saved.getOverlayImageWidth(),
                        saved.getOverlayImageHeight()
                );
            } catch (RuntimeException exception) {
                if (uploadAttempted) {
                    try {
                        mapImageStoragePort.delete(objectKey);
                    } catch (RuntimeException cleanupException) {
                        log.warn(
                                "Failed to clean up overlay image key={}",
                                objectKey,
                                cleanupException
                        );
                    }
                }
                throw exception;
            }
        }
    }

    /** 교체로 밀려난 이전 오버레이 이미지를 지운다. 지우지 못해도 교체 자체는 되돌리지 않는다. */
    private void deleteReplacedImage(MapImageObjectKey previousImageKey, String newObjectKey) {
        if (previousImageKey == null) {
            return;
        }
        String previousKey = previousImageKey.getValue();
        if (previousKey == null || previousKey.equals(newObjectKey)) {
            return;
        }
        try {
            mapImageStoragePort.delete(previousKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete replaced overlay image key={}", previousKey, exception);
        }
    }

    private FestivalMap authorize(
            UUID festivalPublicId,
            UUID mapPublicId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount admin = adminAccountService.getById(principal.adminId());
        if (!admin.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        if (festival.getStatus() != FestivalStatus.DRAFT) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        AdminFestivalRole role = roleService.getByAdminAccountIdAndFestivalId(
                admin.getId(),
                festival.getId()
        );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        FestivalMap map = mapService.getByPublicId(mapPublicId);
        if (!map.belongsTo(festival.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }
        map.validateReadable();
        FestivalRoadmap roadmap = roadmapService.getByFestivalId(festival.getId());
        if (!roadmap.getCurrentMapId().equals(map.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        return map;
    }

    /**
     * IMAGE 지도는 AI 앵커를, COORDINATE 지도는 축제 대표 위치 기본 앵커를 초기값으로 쓴다.
     * 이미 presentation에 표시 앵커가 있으면 {@link FestivalMapPresentationService}가 유지한다.
     */
    private MapImageAnchor resolveFallbackAnchor(FestivalMap map) {
        if (map.getImageAnchor() != null) {
            return map.getImageAnchor();
        }
        FestivalLocation primary = festivalLocationService.findAllByFestivalId(map.getFestivalId())
                .stream()
                .filter(FestivalLocation::isPrimary)
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_LOCATION_REQUIRED));
        return MapImageAnchor.defaultAt(primary.getLatitude(), primary.getLongitude());
    }

    private String objectKey(
            UUID festivalPublicId,
            UUID mapPublicId,
            UUID assetId,
            String extension
    ) {
        return ROOT_PREFIX
                + "/" + festivalPublicId
                + "/maps/" + mapPublicId
                + "/overlay/" + assetId
                + "." + extension;
    }
}
