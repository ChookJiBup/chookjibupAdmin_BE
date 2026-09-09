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
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

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

        @Test
        @DisplayName("시리즈에 묶이지 않은 공공데이터 축제도 조회한다")
        void success_Search_UnlinkedFestival() {
            // given
            Festival unlinked = persistUnlinkedFestival("강릉커피축제", 2025);

            // when
            List<FestivalSeriesSearchView> result =
                    queryRepository.search("커피축제", 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().seriesId()).isNull();
            assertThat(result.getFirst().name()).isEqualTo("강릉커피축제");
            assertThat(result.getFirst().latestFestivalId())
                    .isEqualTo(unlinked.getPublicId());
            assertThat(result.getFirst().latestYear()).isEqualTo(2025);
        }

        @Test
        @DisplayName("같은 이름의 미연결 축제는 가장 최근 회차만 조회한다")
        void success_Search_UnlinkedLatestYearOnly() {
            // given
            persistUnlinkedFestival("강릉커피축제", 2025);
            Festival latest = persistUnlinkedFestival("강릉 커피축제", 2026);

            // when
            List<FestivalSeriesSearchView> result =
                    queryRepository.search("커피축제", 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().latestFestivalId())
                    .isEqualTo(latest.getPublicId());
            assertThat(result.getFirst().latestYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("같은 이름이면 등록된 시리즈를 미연결 축제보다 우선한다")
        void success_Search_SeriesPreferredOverUnlinked() {
            // given
            FestivalSeries series = persistSeries("강릉커피축제");
            persistFestival(series, 2026);
            persistUnlinkedFestival("강릉커피축제", 2025);

            // when
            List<FestivalSeriesSearchView> result =
                    queryRepository.search("강릉커피축제", 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().seriesId())
                    .isEqualTo(series.getPublicId());
            assertThat(result.getFirst().latestYear()).isEqualTo(2026);
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
        Festival festival = createFestival(
                series.getId(),
                series.getPublicId(),
                series.getNameValue(),
                year
        );
        entityManager.persist(festival);
        entityManager.flush();
        return festival;
    }

    /**
     * 공공데이터 파이프라인이 적재한 축제를 흉내 낸다.
     *
     * <p>파이프라인은 {@code series_id}를 채우지 않지만 도메인 생성자는 시리즈를
     * 요구하므로, 생성 후 시리즈 값만 비워 저장한다.</p>
     */
    private Festival persistUnlinkedFestival(String name, int year) {
        Festival festival = createFestival(1L, UUID.randomUUID(), name, year);
        ReflectionTestUtils.setField(festival, "seriesId", null);
        ReflectionTestUtils.setField(festival, "seriesPublicId", null);
        entityManager.persist(festival);
        entityManager.flush();
        return festival;
    }

    private Festival createFestival(
            Long seriesId,
            UUID seriesPublicId,
            String name,
            int year
    ) {
        return Festival.create(
                seriesId,
                seriesPublicId,
                FestivalName.of(name),
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
    }
}
