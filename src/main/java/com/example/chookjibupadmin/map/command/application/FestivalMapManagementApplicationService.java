package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.FestivalMapDeletionTarget;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 축제 도면 원본·표시본·AI 분석본의 교체·삭제 순서를 조정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalMapManagementApplicationService {

    private static final String ROOT_PREFIX = "private/festivals";

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalMapService festivalMapService;
    private final FestivalMapLifecycleApplicationService lifecycleService;
    private final MapImagePreparationPort imagePreparationPort;
    private final MapImageStoragePort imageStoragePort;

    public FestivalMap replace(
            UUID festivalPublicId,
            UUID currentMapId,
            String mapName,
            MapImageUploadCommand imageCommand,
            AdminPrincipal principal
    ) {
        AuthorizedFestival authorized = authorize(festivalPublicId, principal);
        FestivalMap current = festivalMapService.getByPublicId(currentMapId);
        validateOwnership(current, authorized.festivalId());
        FestivalMapName replacementName = mapName == null
                ? current.getMapName()
                : FestivalMapName.of(mapName);
        UUID replacementMapId = UUID.randomUUID();

        try (PreparedMapImage prepared = imagePreparationPort.prepare(imageCommand)) {
            String originalKey = objectKey(
                    festivalPublicId,
                    replacementMapId,
                    "original",
                    UUID.randomUUID(),
                    prepared.originalExtension()
            );
            String displayKey = objectKey(
                    festivalPublicId,
                    replacementMapId,
                    "display",
                    UUID.randomUUID(),
                    prepared.displayExtension()
            );
            String analysisKey = objectKey(
                    festivalPublicId,
                    replacementMapId,
                    "analysis",
                    UUID.randomUUID(),
                    prepared.analysisExtension()
            );
            boolean originalAttempted = false;
            boolean displayAttempted = false;
            boolean analysisAttempted = false;
            try {
                originalAttempted = true;
                uploadOriginal(originalKey, prepared);
                displayAttempted = true;
                uploadDisplay(displayKey, prepared);
                analysisAttempted = true;
                uploadAnalysis(analysisKey, prepared);
                FestivalMap replacement = uploadedMap(
                        authorized,
                        replacementMapId,
                        replacementName,
                        originalKey,
                        displayKey,
                        analysisKey,
                        prepared
                );
                AuthorizedFestival commitAuthorization = authorize(
                        festivalPublicId,
                        principal
                );
                if (!authorized.equals(commitAuthorization)) {
                    throw new CustomException(ErrorCode.FORBIDDEN);
                }
                return lifecycleService.replace(
                        currentMapId,
                        authorized.festivalId(),
                        replacement
                );
            } catch (RuntimeException exception) {
                compensate(analysisKey, analysisAttempted);
                compensate(displayKey, displayAttempted);
                compensate(originalKey, originalAttempted);
                throw exception;
            }
        }
    }

    public void delete(
            UUID festivalPublicId,
            UUID mapId,
            AdminPrincipal principal
    ) {
        AuthorizedFestival authorized = authorize(festivalPublicId, principal);
        FestivalMapDeletionTarget target = lifecycleService.beginDeletion(
                mapId,
                authorized.festivalId()
        );
        imageStoragePort.delete(target.originalObjectKey());
        imageStoragePort.delete(target.displayObjectKey());
        imageStoragePort.delete(target.analysisObjectKey());
        lifecycleService.completeDeletion(mapId, authorized.festivalId());
    }

    private AuthorizedFestival authorize(
            UUID festivalPublicId,
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
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(admin.getId(), festival.getId());
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return new AuthorizedFestival(festival.getId(), admin.getId());
    }

    private void validateOwnership(FestivalMap map, Long festivalId) {
        if (!map.belongsTo(festivalId)) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }
    }

    private FestivalMap uploadedMap(
            AuthorizedFestival authorized,
            UUID mapId,
            FestivalMapName mapName,
            String originalKey,
            String displayKey,
            String analysisKey,
            PreparedMapImage prepared
    ) {
        return FestivalMap.uploaded(
                mapId,
                authorized.festivalId(),
                mapName,
                MapImageFileName.of(prepared.originalFileName()),
                MapImageObjectKey.of(originalKey),
                MapImageObjectKey.of(displayKey),
                MapImageObjectKey.of(analysisKey),
                MapImageContentType.of(prepared.originalContentType()),
                MapImageContentType.of(prepared.displayContentType()),
                MapImageContentType.of(prepared.analysisContentType()),
                MapImageFileSize.of(prepared.originalFileSize()),
                MapImageFileSize.of(prepared.displayFileSize()),
                MapImageFileSize.of(prepared.analysisFileSize()),
                MapImageDimensions.of(
                        prepared.displayImageWidth(),
                        prepared.displayImageHeight()
                ),
                MapImageDimensions.of(
                        prepared.analysisImageWidth(),
                        prepared.analysisImageHeight()
                ),
                Sha256Checksum.of(prepared.originalChecksumSha256()),
                Sha256Checksum.of(prepared.displayChecksumSha256()),
                Sha256Checksum.of(prepared.analysisChecksumSha256()),
                authorized.adminId()
        );
    }

    private void uploadOriginal(String key, PreparedMapImage prepared) {
        imageStoragePort.upload(new StoredMapImageFile(
                key,
                prepared.originalPath(),
                prepared.originalFileSize(),
                prepared.originalContentType(),
                prepared.originalChecksumSha256()
        ));
    }

    private void uploadDisplay(String key, PreparedMapImage prepared) {
        imageStoragePort.upload(new StoredMapImageFile(
                key,
                prepared.displayPath(),
                prepared.displayFileSize(),
                prepared.displayContentType(),
                prepared.displayChecksumSha256()
        ));
    }

    private void uploadAnalysis(String key, PreparedMapImage prepared) {
        imageStoragePort.upload(new StoredMapImageFile(
                key,
                prepared.analysisPath(),
                prepared.analysisFileSize(),
                prepared.analysisContentType(),
                prepared.analysisChecksumSha256()
        ));
    }

    private String objectKey(
            UUID festivalId,
            UUID mapId,
            String variant,
            UUID assetId,
            String extension
    ) {
        return "%s/%s/maps/%s/%s/%s.%s".formatted(
                ROOT_PREFIX, festivalId, mapId, variant, assetId, extension
        );
    }

    private void compensate(String objectKey, boolean attempted) {
        if (!attempted || objectKey == null) {
            return;
        }
        try {
            imageStoragePort.delete(objectKey);
        } catch (RuntimeException cleanupException) {
            log.error("Festival map compensation failed: objectKey={}", objectKey,
                    cleanupException);
        }
    }

    private record AuthorizedFestival(Long festivalId, Long adminId) {
    }
}
