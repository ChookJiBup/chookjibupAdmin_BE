package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class FestivalSeriesJpaRepositoryTest {

    @Autowired
    private FestivalSeriesJpaRepository festivalSeriesJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("축제 묶음을 DB에 저장한다")
        void success_Save() {
            // given
            FestivalSeries festivalSeries = festivalSeries();

            // when
            FestivalSeries saved =
                    festivalSeriesJpaRepository.saveAndFlush(festivalSeries);
            entityManager.clear();
            FestivalSeries found = festivalSeriesJpaRepository
                    .findById(saved.getId())
                    .orElseThrow();

            // then
            assertThat(found.getId()).isNotNull();
            assertThat(found.getPublicId()).isNotNull();
            assertThat(found.getNameValue()).isEqualTo("마포나루 새우젓축제");
            assertThat(found.getNormalizedName()).isEqualTo("마포나루새우젓축제");
        }

        @Test
        @DisplayName("파이프라인 SQL의 시리즈 컬럼명으로 저장한다")
        void success_Save_PipelineColumnNames() {
            FestivalSeries saved =
                    festivalSeriesJpaRepository.saveAndFlush(festivalSeries());

            Object[] row = (Object[]) entityManager.createNativeQuery("""
                            select series_id,
                                   series_name,
                                   normalized_name,
                                   public_id
                            from festival_series
                            where series_id = :seriesId
                            """)
                    .setParameter("seriesId", saved.getId())
                    .getSingleResult();

            assertThat(row[0]).isEqualTo(saved.getId());
            assertThat(row[1]).isEqualTo("마포나루 새우젓축제");
            assertThat(row[2]).isEqualTo("마포나루새우젓축제");
            assertThat(row[3]).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("축제 묶음 UUID로 DB에서 조회한다")
        void success_FindByPublicId() {
            // given
            FestivalSeries saved =
                    festivalSeriesJpaRepository.saveAndFlush(festivalSeries());
            entityManager.clear();

            // when
            var found = festivalSeriesJpaRepository.findByPublicId(saved.getPublicId());

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
        @DisplayName("정규화된 축제명으로 DB에서 조회한다")
        void success_FindByNormalizedName() {
            // given
            festivalSeriesJpaRepository.saveAndFlush(festivalSeries());
            entityManager.clear();

            // when
            var found = festivalSeriesJpaRepository.findByNormalizedName(
                    "마포나루새우젓축제"
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(FestivalSeries::getNameValue)
                    .isEqualTo("마포나루 새우젓축제");
        }
    }

    private FestivalSeries festivalSeries() {
        return FestivalSeries.create(
                FestivalName.of("마포나루 새우젓축제")
        );
    }
}
