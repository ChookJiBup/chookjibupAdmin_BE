package com.example.chookjibupadmin.map.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.util.UUID;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FestivalMapTest {

    @Test
    @DisplayName("S3 저장이 완료된 최초 배치도를 현재 배치도로 생성한다")
    void success_Uploaded() {
        FestivalMap festivalMap = festivalMap();

        assertThat(festivalMap.getPublicId()).isNotNull();
        assertThat(festivalMap.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.UPLOADED);
        assertThat(festivalMap.isCurrent()).isTrue();
        assertThat(festivalMap.getDisplayImageDimensions().getWidth()).isEqualTo(1200);
        assertThat(festivalMap.getDisplayImageDimensions().getHeight()).isEqualTo(800);
        assertThat(festivalMap.getAnalysisImageDimensions().getWidth()).isEqualTo(1200);
    }

    @Test
    @DisplayName("배치도 저장 메타데이터가 비어 있으면 생성할 수 없다")
    void fail_Uploaded_BlankKey() {
        assertThatThrownBy(() -> FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                FestivalMapName.of("축제 배치도"),
                MapImageFileName.of("map.png"),
                null,
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100L),
                MapImageFileSize.of(100L),
                MapImageFileSize.of(90L),
                MapImageDimensions.of(1200, 800),
                MapImageDimensions.of(1200, 800),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)),
                2L
        )).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("새 배치도로 교체하면 기존 배치도를 이력 상태로 전환한다")
    void success_ReplaceWith() {
        FestivalMap current = festivalMap();
        FestivalMap replacement = festivalMap();
        ReflectionTestUtils.setField(current, "id", 10L);
        LocalDateTime replacedAt = LocalDateTime.now();

        current.replaceWith(replacement, replacedAt);

        assertThat(current.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.REPLACED);
        assertThat(current.isCurrent()).isFalse();
        assertThat(current.getReplacedAt()).isEqualTo(replacedAt);
        assertThat(replacement.getReplacesMapId()).isEqualTo(10L);
        assertThat(replacement.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("삭제 시작 후 완료하면 삭제 시각과 상태를 확정한다")
    void success_CompleteDeletion() {
        FestivalMap festivalMap = festivalMap();
        LocalDateTime deletedAt = LocalDateTime.now();

        festivalMap.beginDeletion();
        festivalMap.completeDeletion(deletedAt);

        assertThat(festivalMap.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.DELETED);
        assertThat(festivalMap.isCurrent()).isFalse();
        assertThat(festivalMap.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("삭제 완료 상태에서 삭제를 재시도해도 성공 상태를 유지한다")
    void success_CompleteDeletion_Idempotent() {
        FestivalMap festivalMap = festivalMap();
        LocalDateTime firstDeletedAt = LocalDateTime.now();
        festivalMap.beginDeletion();
        festivalMap.completeDeletion(firstDeletedAt);

        festivalMap.beginDeletion();
        festivalMap.completeDeletion(firstDeletedAt.plusMinutes(1));

        assertThat(festivalMap.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.DELETED);
        assertThat(festivalMap.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("업로드가 완료된 현재 배치도는 화면에 표시할 수 있다")
    void success_ValidateReadable() {
        FestivalMap festivalMap = festivalMap();

        festivalMap.validateReadable();

        assertThat(festivalMap.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("교체 이력 배치도는 현재 화면 조회 URL을 발급할 수 없다")
    void fail_ValidateReadable_Replaced_CustomException() {
        FestivalMap current = festivalMap();
        ReflectionTestUtils.setField(current, "id", 10L);
        current.replaceWith(festivalMap(), LocalDateTime.now());

        assertThatThrownBy(current::validateReadable)
                .isInstanceOf(CustomException.class);
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                FestivalMapName.of("축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100L),
                MapImageFileSize.of(90L),
                MapImageFileSize.of(80L),
                MapImageDimensions.of(1200, 800),
                MapImageDimensions.of(1200, 800),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)),
                2L
        );
    }
}
