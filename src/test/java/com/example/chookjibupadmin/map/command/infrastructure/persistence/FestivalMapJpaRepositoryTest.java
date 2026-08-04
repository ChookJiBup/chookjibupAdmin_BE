package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMapStorageStatus;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class FestivalMapJpaRepositoryTest {

    @Autowired
    private FestivalMapJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("축제 도면 original, display, analysis 메타데이터를 저장한다")
    void success_Save() {
        FestivalMap saved = repository.saveAndFlush(festivalMap());
        UUID publicId = saved.getPublicId();
        entityManager.clear();

        FestivalMap found = repository.findByPublicId(publicId).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getOriginalImageKey().getValue()).isEqualTo("original-key");
        assertThat(found.getDisplayImageKey().getValue()).isEqualTo("display-key");
        assertThat(found.getAnalysisImageKey().getValue()).isEqualTo("analysis-key");
        assertThat(found.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.UPLOADED);
        assertThat(found.isCurrent()).isTrue();
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                FestivalMapName.of("테스트 축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100),
                MapImageFileSize.of(90),
                MapImageFileSize.of(80),
                MapImageDimensions.of(800, 600),
                MapImageDimensions.of(800, 600),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)),
                2L
        );
    }
}
