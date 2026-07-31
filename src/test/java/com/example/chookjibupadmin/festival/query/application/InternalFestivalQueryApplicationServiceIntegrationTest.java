package com.example.chookjibupadmin.festival.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalProgressStatus;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryView;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InternalFestivalQueryApplicationServiceIntegrationTest {

    @Autowired
    private InternalFestivalQueryApplicationService queryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Nested
    @DisplayName("searchFestivals")
    class SearchFestivals {

        @Test
        @DisplayName("진행 상태별 사용자 서버용 축제 목록을 조회한다")
        void success_SearchFestivals_ByProgressStatus() {
            // given
            LocalDate today = LocalDate.now(clock);
            persist(festival("진행 예정 축제", 1L, today.plusDays(1), today.plusDays(3)));
            persist(festival("진행 중 축제", 2L, today.minusDays(1), today.plusDays(1)));
            persist(festival("진행 완료 축제", 3L, today.minusDays(3), today.minusDays(1)));

            // when
            var result = queryService.searchFestivals(
                    InternalFestivalProgressStatus.ONGOING,
                    null,
                    null,
                    null
            );

            // then
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryView::name)
                    .containsExactly("진행 중 축제");
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryView::progressStatus)
                    .containsExactly(InternalFestivalProgressStatus.ONGOING);
        }

        @Test
        @DisplayName("검색어와 페이지 조건으로 사용자 서버용 축제 목록을 조회한다")
        void success_SearchFestivals_ByKeywordAndPage() {
            // given
            LocalDate today = LocalDate.now(clock);
            persist(festival("마포나루 새우젓축제", 1L, today.plusDays(1), today.plusDays(3)));
            persist(festival("서울빛초롱축제", 2L, today.plusDays(2), today.plusDays(4)));

            // when
            var result = queryService.searchFestivals(
                    InternalFestivalProgressStatus.UPCOMING,
                    "새우젓",
                    0,
                    1
            );

            // then
            assertThat(result.getContent())
                    .extracting(InternalFestivalSummaryView::name)
                    .containsExactly("마포나루 새우젓축제");
            assertThat(result.getTotalElements()).isEqualTo(1);
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
        return Festival.create(
                seriesId,
                UUID.randomUUID(),
                FestivalName.of(name),
                FestivalDescription.of("지역 축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(startDate, endDate),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
