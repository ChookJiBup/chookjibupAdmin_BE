package com.example.chookjibupadmin.map.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FestivalMapTest {

    @Test
    @DisplayName("S3 저장이 완료된 최초 배치도를 현재 배치도로 생성한다")
    void success_Uploaded() {
        FestivalMap festivalMap = festivalMap();

        assertThat(festivalMap.getPublicId()).isNotNull();
        assertThat(festivalMap.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.UPLOADED);
        assertThat(festivalMap.isCurrent()).isTrue();
        assertThat(festivalMap.getImageWidth()).isEqualTo(1200);
        assertThat(festivalMap.getImageHeight()).isEqualTo(800);
    }

    @Test
    @DisplayName("배치도 저장 메타데이터가 비어 있으면 생성할 수 없다")
    void fail_Uploaded_BlankKey() {
        assertThatThrownBy(() -> FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                "축제 배치도",
                "map.png",
                "",
                "display-key",
                "image/png",
                "image/png",
                100L,
                100L,
                1200,
                800,
                "a".repeat(64),
                "b".repeat(64),
                2L
        )).isInstanceOf(CustomException.class);
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                "축제 배치도",
                "map.png",
                "source-key",
                "display-key",
                "image/png",
                "image/png",
                100L,
                90L,
                1200,
                800,
                "a".repeat(64),
                "b".repeat(64),
                2L
        );
    }
}
