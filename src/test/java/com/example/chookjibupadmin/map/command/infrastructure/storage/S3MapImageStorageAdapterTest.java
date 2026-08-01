package com.example.chookjibupadmin.map.command.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3MapImageStorageAdapterTest {

    @Mock
    private S3Client mapS3Client;

    private S3MapImageStorageAdapter adapter() {
        return new S3MapImageStorageAdapter(
                mapS3Client,
                new MapStorageProperties(
                        "s3",
                        "festival-assets-test",
                        "ap-northeast-2",
                        URI.create("http://localhost:4566"),
                        true,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(5)
                )
        );
    }

    @Test
    @DisplayName("검증된 이미지 파일을 비공개 S3 Bucket에 저장한다")
    void success_Upload() throws Exception {
        Path path = Files.createTempFile("s3-adapter-test-", ".png");
        try {
            Files.write(path, new byte[]{1, 2, 3});
            org.mockito.BDDMockito.given(mapS3Client.putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class)
            )).willReturn(PutObjectResponse.builder().build());

            adapter().upload(file(path));

            then(mapS3Client).should().putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class)
            );
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @DisplayName("S3 오류를 내부 배치도 업로드 오류로 변환한다")
    void fail_Upload_S3Exception() throws Exception {
        Path path = Files.createTempFile("s3-adapter-test-", ".png");
        try {
            Files.write(path, new byte[]{1, 2, 3});
            doThrow(S3Exception.builder().message("failed").build())
                    .when(mapS3Client)
                    .putObject(any(PutObjectRequest.class), any(RequestBody.class));

            assertThatThrownBy(() -> adapter().upload(file(path)))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_MAP_UPLOAD_FAILED.getMessage());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private StoredMapImageFile file(Path path) throws Exception {
        return new StoredMapImageFile(
                "private/festivals/test/maps/test/source/test.png",
                path,
                Files.size(path),
                "image/png",
                "a".repeat(64)
        );
    }
}
