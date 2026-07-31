package com.example.chookjibupadmin.festival.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.chookjibupadmin.festival.query.repository.FestivalSeriesQueryRepository;
import com.example.chookjibupadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({FestivalSeriesQueryRepositoryImpl.class, QuerydslConfig.class})
class FestivalSeriesQueryRepositoryTest {

    @Autowired
    private FestivalSeriesQueryRepository queryRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("축제명 일부로 시리즈와 가장 최근 개최 정보를 조회한다")
        void success_Search_LatestFestival() {
            // given
            FestivalSeries series = persistSeries("김밥축제");
            persistFestival(series, 2025);
            Festival latest = persistFestival(series, 2026);
            persistSeries("새우젓축제");

            // when
            List<FestivalSeriesSearchView> result =
                    queryRepository.search("김밥", 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().seriesId())
                    .isEqualTo(series.getPublicId());
            assertThat(result.getFirst().latestFestivalId())
                    .isEqualTo(latest.getPublicId());
            assertThat(result.getFirst().latestYear()).isEqualTo(2026);
            assertThat(result.getFirst().latestDetailAddress())
                    .isEqualTo("월드컵공원");
        }

        @Test
        @DisplayName("개최 이력이 없는 시리즈도 최근 정보가 없는 상태로 조회한다")
        void success_Search_EmptyLatestFestivalBoundary() {
            // given
            FestivalSeries series = persistSeries("김밥축제");

            // when
            List<FestivalSeriesSearchView> result =
                    queryRepository.search("김밥", 1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().seriesId())
                    .isEqualTo(series.getPublicId());
            assertThat(result.getFirst().latestFestivalId()).isNull();
            assertThat(result.getFirst().latestYear()).isNull();
        }
    }

    private FestivalSeries persistSeries(String name) {
        FestivalSeries series = FestivalSeries.create(FestivalName.of(name));
        entityManager.persist(series);
        entityManager.flush();
        return series;
    }

    private Festival persistFestival(
            FestivalSeries series,
            int year
    ) {
        Festival festival = Festival.create(
                series.getId(),
                series.getPublicId(),
                FestivalName.of(series.getNameValue()),
                FestivalDescription.of(year + "년 축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(
                        LocalDate.of(year, 10, 1),
                        LocalDate.of(year, 10, 3)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
        entityManager.persist(festival);
        entityManager.flush();
        return festival;
    }
}
