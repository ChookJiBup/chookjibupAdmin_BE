package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.dto.UploadedFestivalMap;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
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
 * 축제 등록 도면의 원본·표시본·AI 분석본 저장과 DB 등록 순서를 조정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalMapRegistrationApplicationService {

    private static final String ROOT_PREFIX = "private/festivals";

    private final AdminAccountService adminAccountService;
    private final MapImagePreparationPort mapImagePreparationPort;
    private final MapImageStoragePort mapImageStoragePort;
    private final FestivalApplicationService festivalApplicationService;

    public CreateFestivalWithMapResult create(
            CreateFestivalCommand command,
            MapImageUploadCommand imageCommand,
            AdminPrincipal principal
    ) {
        validateCreator(principal);
        UUID festivalPublicId = UUID.randomUUID();
        UUID mapPublicId = UUID.randomUUID();
        UUID originalAssetId = UUID.randomUUID();
        UUID displayAssetId = UUID.randomUUID();
        UUID analysisAssetId = UUID.randomUUID();

        try (PreparedMapImage prepared = mapImagePreparationPort.prepare(
                imageCommand
        )) {
            String originalKey = objectKey(
                    festivalPublicId,
                    mapPublicId,
                    "original",
                    originalAssetId,
                    prepared.originalExtension()
            );
            String displayKey = objectKey(
                    festivalPublicId,
                    mapPublicId,
                    "display",
                    displayAssetId,
                    prepared.displayExtension()
            );
            String analysisKey = objectKey(
                    festivalPublicId,
                    mapPublicId,
                    "analysis",
                    analysisAssetId,
                    prepared.analysisExtension()
            );
            boolean originalAttempted = false;
            boolean displayAttempted = false;
            boolean analysisAttempted = false;
            try {
                originalAttempted = true;
                mapImageStoragePort.upload(new StoredMapImageFile(
                        originalKey,
                        prepared.originalPath(),
                        prepared.originalFileSize(),
                        prepared.originalContentType(),
                        prepared.originalChecksumSha256()
                ));
                displayAttempted = true;
                mapImageStoragePort.upload(new StoredMapImageFile(
                        displayKey,
                        prepared.displayPath(),
                        prepared.displayFileSize(),
                        prepared.displayContentType(),
                        prepared.displayChecksumSha256()
                ));
                analysisAttempted = true;
                mapImageStoragePort.upload(new StoredMapImageFile(
                        analysisKey,
                        prepared.analysisPath(),
                        prepared.analysisFileSize(),
                        prepared.analysisContentType(),
                        prepared.analysisChecksumSha256()
                ));
                return festivalApplicationService.createWithMap(
                        command,
                        principal,
                        festivalPublicId,
                        new UploadedFestivalMap(
                                mapPublicId,
                                FestivalMapName.of(
                                        command.name().trim() + " 배치도"
                                ),
                                MapImageFileName.of(
                                        prepared.originalFileName()
                                ),
                                MapImageObjectKey.of(originalKey),
                                MapImageObjectKey.of(displayKey),
                                MapImageObjectKey.of(analysisKey),
                                MapImageContentType.of(
                                        prepared.originalContentType()
                                ),
                                MapImageContentType.of(
                                        prepared.displayContentType()
                                ),
                                MapImageContentType.of(
                                        prepared.analysisContentType()
                                ),
                                MapImageFileSize.of(
                                        prepared.originalFileSize()
                                ),
                                MapImageFileSize.of(
                                        prepared.displayFileSize()
                                ),
                                MapImageFileSize.of(
                                        prepared.analysisFileSize()
                                ),
                                MapImageDimensions.of(
                                        prepared.displayImageWidth(),
                                        prepared.displayImageHeight()
                                ),
                                MapImageDimensions.of(
                                        prepared.analysisImageWidth(),
                                        prepared.analysisImageHeight()
                                ),
                                Sha256Checksum.of(
                                        prepared.originalChecksumSha256()
                                ),
                                Sha256Checksum.of(
                                        prepared.displayChecksumSha256()
                                ),
                                Sha256Checksum.of(
                                        prepared.analysisChecksumSha256()
                                )
                        )
                );
            } catch (RuntimeException exception) {
                compensate(analysisKey, analysisAttempted);
                compensate(displayKey, displayAttempted);
                compensate(originalKey, originalAttempted);
                throw exception;
            }
        }
    }

    private void validateCreator(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount creator = adminAccountService.getById(principal.adminId());
        if (!creator.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
    }

    private String objectKey(
            UUID festivalId,
            UUID mapId,
            String variant,
            UUID assetId,
            String extension
    ) {
        return "%s/%s/maps/%s/%s/%s.%s".formatted(
                ROOT_PREFIX,
                festivalId,
                mapId,
                variant,
                assetId,
                extension
        );
    }

    private void compensate(String objectKey, boolean uploaded) {
        if (!uploaded) {
            return;
        }
        try {
            mapImageStoragePort.delete(objectKey);
        } catch (RuntimeException cleanupException) {
            log.error(
                    "Festival map compensation failed: objectKey={}",
                    objectKey,
                    cleanupException
            );
        }
    }
}
