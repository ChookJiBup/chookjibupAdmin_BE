package com.example.chookjibupadmin.map.command.infrastructure.storage;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 비공개 S3 버킷에 축제 배치도 이미지를 저장한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.map.storage",
        name = "provider",
        havingValue = "s3"
)
public class S3MapImageStorageAdapter implements MapImageStoragePort {

    private final S3Client mapS3Client;
    private final MapStorageProperties properties;

    @Override
    public void upload(StoredMapImageFile imageFile) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(imageFile.objectKey())
                    .contentType(imageFile.contentType())
                    .contentLength(imageFile.contentLength())
                    .metadata(java.util.Map.of(
                            "checksum-sha256",
                            imageFile.checksumSha256()
                    ))
                    .build();
            mapS3Client.putObject(
                    request,
                    RequestBody.fromFile(imageFile.path())
            );
        } catch (Exception exception) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_MAP_UPLOAD_FAILED,
                    exception
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            mapS3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_MAP_DELETE_FAILED,
                    exception
            );
        }
    }
}
