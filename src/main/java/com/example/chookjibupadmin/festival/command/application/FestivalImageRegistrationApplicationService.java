package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.application.dto.PreparedFestivalThumbnail;
import com.example.chookjibupadmin.festival.command.application.port.FestivalThumbnailPreparationPort;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalImageObjectKey;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 축제 대표 이미지를 지도 분석과 분리된 썸네일로 저장한 뒤 축제 생성과 연결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalImageRegistrationApplicationService {

    private final FestivalThumbnailPreparationPort thumbnailPreparationPort;
    private final MapImageStoragePort imageStoragePort;
    private final FestivalApplicationService festivalApplicationService;

    public Festival create(
            CreateFestivalCommand command,
            FestivalThumbnailUploadCommand imageCommand,
            String publicImageBaseUrl,
            AdminPrincipal principal
    ) {
        UUID festivalPublicId = UUID.randomUUID();
        String objectKey = FestivalImageObjectKey.representative(festivalPublicId);
        boolean uploadAttempted = false;

        try (PreparedFestivalThumbnail prepared =
                     thumbnailPreparationPort.prepare(imageCommand)) {
            try {
                uploadAttempted = true;
                imageStoragePort.upload(new StoredMapImageFile(
                        objectKey,
                        prepared.path(),
                        prepared.fileSize(),
                        prepared.contentType(),
                        prepared.checksumSha256()
                ));
                return festivalApplicationService.createWithImage(
                        command,
                        principal,
                        festivalPublicId,
                        "%s/%s/image".formatted(
                                publicImageBaseUrl.replaceAll("/+$", ""),
                                festivalPublicId
                        )
                );
            } catch (RuntimeException exception) {
                if (uploadAttempted) {
                    compensate(objectKey);
                }
                throw exception;
            }
        }
    }

    private void compensate(String objectKey) {
        try {
            imageStoragePort.delete(objectKey);
        } catch (RuntimeException cleanupException) {
            log.error("Festival image compensation failed: objectKey={}", objectKey, cleanupException);
        }
    }
}
