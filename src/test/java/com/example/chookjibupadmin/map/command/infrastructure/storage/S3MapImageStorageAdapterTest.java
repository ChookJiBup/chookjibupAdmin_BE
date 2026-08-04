package com.example.chookjibupadmin.map.command.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import java.net.URI;
import java.net.URL;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3MapImageStorageAdapterTest {

    @Mock
    private S3Client mapS3Client;

    @Mock
    private S3Presigner mapS3Presigner;

    private S3MapImageStorageAdapter adapter() {
        return new S3MapImageStorageAdapter(
                mapS3Client,
                mapS3Presigner,
                new MapStorageProperties(
                        "s3",
                        "festival-assets-test",
                        "ap-northeast-2",
                        URI.create("http://localhost:4566"),
                        true,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(5),
                        Duration.ofMinutes(10)
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

    @Test
    @DisplayName("화면 표시용 S3 Presigned GET URL을 생성한다")
    void success_CreateReadUrl() throws Exception {
        PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(
                PresignedGetObjectRequest.class
        );
        given(presigned.url()).willReturn(new URL(
                "https://festival-assets-test.s3.ap-northeast-2.amazonaws.com/display.png"
        ));
        given(mapS3Presigner.presignGetObject(
                any(GetObjectPresignRequest.class)
        )).willReturn(presigned);

        var result = adapter().createReadUrl("display-key");

        assertThat(result.url()).isEqualTo(URI.create(
                "https://festival-assets-test.s3.ap-northeast-2.amazonaws.com/display.png"
        ));
        assertThat(result.expiresAt()).isNotNull();
        then(mapS3Presigner).should().presignGetObject(
                any(GetObjectPresignRequest.class)
        );
    }

    private StoredMapImageFile file(Path path) throws Exception {
        return new StoredMapImageFile(
                "private/festivals/test/maps/test/original/test.png",
                path,
                Files.size(path),
                "image/png",
                "a".repeat(64)
        );
    }
}
