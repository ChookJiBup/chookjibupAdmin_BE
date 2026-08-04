package com.example.chookjibupadmin.map.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapImageValueObjectTest {

    @Test
    @DisplayName("배치도 이름의 앞뒤 공백을 제거한다")
    void success_FestivalMapNameOf_Trim() {
        FestivalMapName name = FestivalMapName.of("  행사장 배치도  ");

        assertThat(name.getValue()).isEqualTo("행사장 배치도");
    }

    @Test
    @DisplayName("150자를 넘는 배치도 이름은 거절한다")
    void fail_FestivalMapNameOf_TooLong_CustomException() {
        assertThatThrownBy(() -> FestivalMapName.of("가".repeat(151)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("파일명은 경로가 제거된 이름만 허용한다")
    void fail_MapImageFileNameOf_Path_CustomException() {
        assertThatThrownBy(() -> MapImageFileName.of("folder/map.png"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Object Key는 안전한 상대 논리 경로만 허용한다")
    void fail_MapImageObjectKeyOf_ParentPath_CustomException() {
        assertThatThrownBy(() -> MapImageObjectKey.of("maps/../map.png"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("JPEG와 PNG MIME 타입을 허용한다")
    void success_MapImageContentTypeOf_Supported() {
        assertThat(MapImageContentType.of(" IMAGE/JPEG ").getValue())
                .isEqualTo("image/jpeg");
        assertThat(MapImageContentType.of("image/png").getValue())
                .isEqualTo("image/png");
    }

    @Test
    @DisplayName("지원하지 않는 MIME 타입은 거절한다")
    void fail_MapImageContentTypeOf_Unsupported_CustomException() {
        assertThatThrownBy(() -> MapImageContentType.of("image/webp"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("파일 크기는 양수여야 한다")
    void fail_MapImageFileSizeOf_Zero_CustomException() {
        assertThatThrownBy(() -> MapImageFileSize.of(0))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("이미지 너비와 높이는 모두 양수여야 한다")
    void fail_MapImageDimensionsOf_Zero_CustomException() {
        assertThatThrownBy(() -> MapImageDimensions.of(800, 0))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("SHA-256 체크섬은 소문자 64자리 16진수여야 한다")
    void fail_Sha256ChecksumOf_Invalid_CustomException() {
        assertThatThrownBy(() -> Sha256Checksum.of("A".repeat(64)))
                .isInstanceOf(CustomException.class);
    }
}
