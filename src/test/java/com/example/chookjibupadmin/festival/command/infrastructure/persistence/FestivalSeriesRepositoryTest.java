package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeriesRepository;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FestivalSeriesRepositoryTest {

    @Autowired
    private FestivalSeriesRepository festivalSeriesRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("축제 묶음을 저장한다")
        void success_Save() {
            // given
            FestivalSeries festivalSeries = festivalSeries();

            // when
            FestivalSeries saved = festivalSeriesRepository.save(festivalSeries);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getNormalizedName()).isEqualTo("마포나루새우젓축제");
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("축제 묶음 UUID로 조회한다")
        void success_FindByPublicId() {
            // given
            FestivalSeries saved = festivalSeriesRepository.save(festivalSeries());

            // when
            var found = festivalSeriesRepository.findByPublicId(saved.getPublicId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(FestivalSeries::getNameValue)
                    .isEqualTo("마포나루 새우젓축제");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("축제 묶음 ID로 조회한다")
        void success_FindById() {
            // given
            FestivalSeries saved = festivalSeriesRepository.save(festivalSeries());

            // when
            var found = festivalSeriesRepository.findById(saved.getId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(FestivalSeries::getNameValue)
                    .isEqualTo("마포나루 새우젓축제");
        }
    }

    @Nested
    @DisplayName("findByNormalizedName")
    class FindByNormalizedName {

        @Test
        @DisplayName("정규화된 축제명으로 조회한다")
        void success_FindByNormalizedName() {
            // given
            festivalSeriesRepository.save(festivalSeries());

            // when
            var found = festivalSeriesRepository.findByNormalizedName(
                    "마포나루새우젓축제"
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(FestivalSeries::getNameValue)
                    .isEqualTo("마포나루 새우젓축제");
        }

        @Test
        @DisplayName("일치하는 축제 묶음이 없으면 빈 결과를 반환한다")
        void success_FindByNormalizedName_NotFoundBoundary() {
            // given
            String normalizedName = "없는축제";

            // when
            var found = festivalSeriesRepository.findByNormalizedName(
                    normalizedName
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    private FestivalSeries festivalSeries() {
        return FestivalSeries.create(
                FestivalName.of("마포나루 새우젓축제")
        );
    }
}
