package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMapStorageStatus;
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
    @DisplayName("축제 배치도 source와 display 메타데이터를 저장한다")
    void success_Save() {
        FestivalMap saved = repository.saveAndFlush(festivalMap());
        UUID publicId = saved.getPublicId();
        entityManager.clear();

        FestivalMap found = repository.findByPublicId(publicId).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getSourceImageKey()).isEqualTo("source-key");
        assertThat(found.getDisplayImageKey()).isEqualTo("display-key");
        assertThat(found.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.UPLOADED);
        assertThat(found.isCurrent()).isTrue();
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                "테스트 축제 배치도",
                "map.png",
                "source-key",
                "display-key",
                "image/png",
                "image/png",
                100,
                90,
                800,
                600,
                "a".repeat(64),
                "b".repeat(64),
                2L
        );
    }
}
