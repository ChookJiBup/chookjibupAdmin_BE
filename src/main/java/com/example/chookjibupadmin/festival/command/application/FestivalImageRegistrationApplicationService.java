package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalImageObjectKey;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 축제 대표 이미지를 표시용 크기로 정규화해 저장한 뒤 축제 생성과 연결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalImageRegistrationApplicationService {

    private final MapImagePreparationPort imagePreparationPort;
    private final MapImageStoragePort imageStoragePort;
    private final FestivalApplicationService festivalApplicationService;

    public Festival create(
            CreateFestivalCommand command,
            MapImageUploadCommand imageCommand,
            String publicImageBaseUrl,
            AdminPrincipal principal
    ) {
        UUID festivalPublicId = UUID.randomUUID();
        String objectKey = FestivalImageObjectKey.representative(festivalPublicId);
        boolean uploadAttempted = false;

        try (PreparedMapImage prepared = imagePreparationPort.prepare(imageCommand)) {
            try {
                uploadAttempted = true;
                imageStoragePort.upload(new StoredMapImageFile(
                        objectKey,
                        prepared.displayPath(),
                        prepared.displayFileSize(),
                        prepared.displayContentType(),
                        prepared.displayChecksumSha256()
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
