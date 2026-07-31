package com.example.chookjibupadmin.festival.command.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalSeriesTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("축제 묶음을 생성한다")
        void success_Create() {
            // given
            FestivalName name = FestivalName.of("마포나루 새우젓축제");

            // when
            FestivalSeries festivalSeries = FestivalSeries.create(name);

            // then
            assertThat(festivalSeries.getPublicId()).isNotNull();
            assertThat(festivalSeries.getNameValue())
                    .isEqualTo("마포나루 새우젓축제");
            assertThat(festivalSeries.getNormalizedName())
                    .isEqualTo("마포나루새우젓축제");
        }
    }

    @Nested
    @DisplayName("normalize")
    class Normalize {

        @Test
        @DisplayName("공백과 대소문자 차이를 제거한다")
        void success_Normalize_IgnoreBlankAndCase() {
            // given
            FestivalName name = FestivalName.of("Mapo Festival");

            // when
            String normalizedName = FestivalSeries.normalize(name);

            // then
            assertThat(normalizedName).isEqualTo("mapofestival");
        }

        @Test
        @DisplayName("최소 길이 축제명도 정규화한다")
        void success_Normalize_MinLengthBoundary() {
            // given
            FestivalName name = FestivalName.of("ab");

            // when
            String normalizedName = FestivalSeries.normalize(name);

            // then
            assertThat(normalizedName).isEqualTo("ab");
        }
    }
}
