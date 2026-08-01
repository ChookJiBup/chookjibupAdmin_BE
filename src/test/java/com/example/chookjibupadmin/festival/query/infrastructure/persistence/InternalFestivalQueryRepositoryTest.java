package com.example.chookjibupadmin.festival.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryProjection;
import com.example.chookjibupadmin.festival.query.repository.InternalFestivalQueryRepository;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({InternalFestivalQueryRepositoryImpl.class, QuerydslConfig.class})
class InternalFestivalQueryRepositoryTest {

    @Autowired
    private InternalFestivalQueryRepository queryRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("searchFestivals")
    class SearchFestivals {

        @Test
        @DisplayName("진행 예정 축제 목록을 조회한다")
        void success_SearchFestivals_Upcoming() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 10);
            persist(festival("진행 예정 축제", 1L, today.plusDays(1), today.plusDays(3)));
            persist(festival("진행 중 축제", 2L, today.minusDays(1), today.plusDays(1)));
            persist(festival("진행 완료 축제", 3L, today.minusDays(3), today.minusDays(1)));

            // when
            var result = queryRepository.searchFestivals(
                    new InternalFestivalSearchCondition(
                            FestivalProgressStatus.UPCOMING,
                            null,
                            today
                    ),
                    PageRequest.of(0, 20)
            );

            // then
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryProjection::name)
                    .containsExactly("진행 예정 축제");
            assertThat(result.getContent().getFirst().detailAddress())
                    .isEqualTo("월드컵공원");
        }

        @Test
        @DisplayName("진행 중 축제는 시작일과 종료일 경계를 포함해 조회한다")
        void success_SearchFestivals_OngoingBoundary() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 10);
            persist(festival("시작일 축제", 1L, today, today.plusDays(2)));
            persist(festival("종료일 축제", 2L, today.minusDays(2), today));
            persist(festival("진행 예정 축제", 3L, today.plusDays(1), today.plusDays(2)));

            // when
            var result = queryRepository.searchFestivals(
                    new InternalFestivalSearchCondition(
                            FestivalProgressStatus.ONGOING,
                            null,
                            today
                    ),
                    PageRequest.of(0, 20)
            );

            // then
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryProjection::name)
                    .containsExactly("종료일 축제", "시작일 축제");
        }

        @Test
        @DisplayName("진행 완료 축제 목록을 조회한다")
        void success_SearchFestivals_Completed() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 10);
            persist(festival("진행 완료 축제", 1L, today.minusDays(3), today.minusDays(1)));
            persist(festival("진행 중 축제", 2L, today.minusDays(1), today.plusDays(1)));

            // when
            var result = queryRepository.searchFestivals(
                    new InternalFestivalSearchCondition(
                            FestivalProgressStatus.COMPLETED,
                            null,
                            today
                    ),
                    PageRequest.of(0, 20)
            );

            // then
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryProjection::name)
                    .containsExactly("진행 완료 축제");
        }

        @Test
        @DisplayName("검색어가 있으면 축제명과 주소로 필터링한다")
        void success_SearchFestivals_ByKeyword() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 10);
            persist(festival(
                    "마포나루 새우젓축제",
                    "서울특별시 마포구 월드컵로 243",
                    1L,
                    today,
                    today.plusDays(1)
            ));
            persist(festival(
                    "서울빛초롱축제",
                    "서울특별시 종로구 청계천로 1",
                    2L,
                    today,
                    today.plusDays(1)
            ));

            // when
            var result = queryRepository.searchFestivals(
                    new InternalFestivalSearchCondition(null, "마포", today),
                    PageRequest.of(0, 20)
            );

            // then
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryProjection::name)
                    .containsExactly("마포나루 새우젓축제");
        }

        @Test
        @DisplayName("페이지 크기만큼 목록과 전체 개수를 반환한다")
        void success_SearchFestivals_PageBoundary() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 10);
            persist(festival("첫 번째 축제", 1L, today.plusDays(1), today.plusDays(2)));
            persist(festival("두 번째 축제", 2L, today.plusDays(2), today.plusDays(3)));

            // when
            var result = queryRepository.searchFestivals(
                    new InternalFestivalSearchCondition(null, null, today),
                    PageRequest.of(0, 1)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("조건에 맞는 축제가 없으면 빈 페이지를 반환한다")
        void success_SearchFestivals_EmptyBoundary() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 10);

            // when
            var result = queryRepository.searchFestivals(
                    new InternalFestivalSearchCondition(
                            FestivalProgressStatus.UPCOMING,
                            null,
                            today
                    ),
                    PageRequest.of(0, 20)
            );

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    private Festival persist(Festival festival) {
        entityManager.persist(festival);
        entityManager.flush();
        return festival;
    }

    private Festival festival(
            String name,
            Long seriesId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return festival(
                name,
                "서울특별시 마포구 월드컵로 243",
                seriesId,
                startDate,
                endDate
        );
    }

    private Festival festival(
            String name,
            String address,
            Long seriesId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Festival.create(
                seriesId,
                UUID.randomUUID(),
                FestivalName.of(name),
                FestivalDescription.of("지역 축제 설명"),
                FestivalAddress.of(address),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(startDate, endDate),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
