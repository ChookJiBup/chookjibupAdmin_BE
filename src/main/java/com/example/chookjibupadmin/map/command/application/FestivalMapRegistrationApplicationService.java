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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 축제 등록 이미지 저장과 DB 등록 순서를 조정한다.
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
        UUID sourceAssetId = UUID.randomUUID();
        UUID displayAssetId = UUID.randomUUID();

        try (PreparedMapImage prepared = mapImagePreparationPort.prepare(
                imageCommand
        )) {
            String sourceKey = objectKey(
                    festivalPublicId,
                    mapPublicId,
                    "source",
                    sourceAssetId,
                    prepared.sourceExtension()
            );
            String displayKey = objectKey(
                    festivalPublicId,
                    mapPublicId,
                    "display",
                    displayAssetId,
                    prepared.displayExtension()
            );
            boolean sourceAttempted = false;
            boolean displayAttempted = false;
            try {
                sourceAttempted = true;
                mapImageStoragePort.upload(new StoredMapImageFile(
                        sourceKey,
                        prepared.sourcePath(),
                        prepared.sourceFileSize(),
                        prepared.sourceContentType(),
                        prepared.sourceChecksumSha256()
                ));
                displayAttempted = true;
                mapImageStoragePort.upload(new StoredMapImageFile(
                        displayKey,
                        prepared.displayPath(),
                        prepared.displayFileSize(),
                        prepared.displayContentType(),
                        prepared.displayChecksumSha256()
                ));
                return festivalApplicationService.createWithMap(
                        command,
                        principal,
                        festivalPublicId,
                        new UploadedFestivalMap(
                                mapPublicId,
                                command.name().trim() + " 배치도",
                                prepared.originalFileName(),
                                sourceKey,
                                displayKey,
                                prepared.sourceContentType(),
                                prepared.displayContentType(),
                                prepared.sourceFileSize(),
                                prepared.displayFileSize(),
                                prepared.imageWidth(),
                                prepared.imageHeight(),
                                prepared.sourceChecksumSha256(),
                                prepared.displayChecksumSha256()
                        )
                );
            } catch (RuntimeException exception) {
                compensate(displayKey, displayAttempted);
                compensate(sourceKey, sourceAttempted);
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
