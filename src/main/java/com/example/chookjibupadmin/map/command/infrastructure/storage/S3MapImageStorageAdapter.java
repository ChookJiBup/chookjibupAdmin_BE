package com.example.chookjibupadmin.map.command.infrastructure.storage;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;
import java.time.Instant;

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
    private final S3Presigner mapS3Presigner;
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

    @Override
    public MapImageReadUrl createReadUrl(String objectKey) {
        Duration ttl = properties.readUrlTtl() == null
                ? Duration.ofMinutes(10)
                : properties.readUrlTtl();
        Instant expiresAt = Instant.now().plus(ttl);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build();
            var presigned = mapS3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(ttl)
                            .getObjectRequest(request)
                            .build()
            );
            return new MapImageReadUrl(
                    presigned.url().toURI(),
                    expiresAt
            );
        } catch (Exception exception) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_MAP_READ_URL_FAILED,
                    exception
            );
        }
    }

    @Override
    public byte[] read(String objectKey, long maxBytes) {
        try {
            long contentLength = mapS3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket()).key(objectKey).build()).contentLength();
            if (contentLength < 0 || contentLength > maxBytes) {
                throw new IllegalArgumentException("stored map image exceeds analysis limit");
            }
            var response = mapS3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.bucket()).key(objectKey).build());
            if (response.asByteArrayUnsafe().length > maxBytes) {
                throw new IllegalArgumentException("stored map image exceeds analysis limit");
            }
            return response.asByteArray();
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_READ_FAILED, exception);
        }
    }
}
