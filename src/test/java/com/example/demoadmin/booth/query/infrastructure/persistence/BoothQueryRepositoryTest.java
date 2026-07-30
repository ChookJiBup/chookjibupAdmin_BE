package com.example.demoadmin.booth.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import com.example.demoadmin.booth.query.repository.BoothQueryRepository;
import com.example.demoadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({BoothQueryRepositoryImpl.class, QuerydslConfig.class})
class BoothQueryRepositoryTest {

    @Autowired
    private BoothQueryRepository boothQueryRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("findAllByFestivalId")
    class FindAllByFestivalId {

        @Test
        @DisplayName("축제별 부스 목록을 Projection으로 조회한다")
        void success_FindAllByFestivalId() {
            // given
            FestivalBooth booth = persist(booth(1L, "푸드 부스"));
            BoothQueueLine queueLine = persist(queueLine(booth.getId(), 1));
            booth.updateQueueTail(queueLine);
            entityManager.flush();
            entityManager.clear();

            // when
            List<BoothView> result = boothQueryRepository.findAllByFestivalId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("푸드 부스");
            assertThat(result.getFirst().currentQueueLineId()).isEqualTo(queueLine.getPublicId());
        }

        @Test
        @DisplayName("다른 축제의 부스는 조회하지 않는다")
        void success_FindAllByFestivalId_FestivalBoundary() {
            // given
            persist(booth(2L, "다른 축제 부스"));
            entityManager.flush();
            entityManager.clear();

            // when
            List<BoothView> result = boothQueryRepository.findAllByFestivalId(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findQueueLinesByFestivalIdAndBoothPublicId")
    class FindQueueLinesByFestivalIdAndBoothPublicId {

        @Test
        @DisplayName("부스 대기 라인 목록을 순서대로 조회한다")
        void success_FindQueueLinesByFestivalIdAndBoothPublicId() {
            // given
            FestivalBooth booth = persist(booth(1L, "푸드 부스"));
            persist(queueLine(booth.getId(), 2));
            persist(queueLine(booth.getId(), 1));
            entityManager.flush();
            entityManager.clear();

            // when
            List<BoothQueueLineView> result =
                    boothQueryRepository.findQueueLinesByFestivalIdAndBoothPublicId(
                    booth.getFestivalId(),
                    booth.getPublicId()
            );

            // then
            assertThat(result)
                    .extracting(BoothQueueLineView::lineOrder)
                    .containsExactly(1, 2);
        }
    }

    private FestivalBooth persist(FestivalBooth booth) {
        entityManager.persist(booth);
        entityManager.flush();
        return booth;
    }

    private BoothQueueLine persist(BoothQueueLine queueLine) {
        entityManager.persist(queueLine);
        entityManager.flush();
        return queueLine;
    }

    private FestivalBooth booth(
            Long festivalId,
            String name
    ) {
        return FestivalBooth.create(
                festivalId,
                BoothName.of(name),
                BoothCategory.of("먹거리"),
                BoothLocation.of("A-1"),
                BoothDescription.of("대표 먹거리 부스")
        );
    }

    private BoothQueueLine queueLine(
            Long boothId,
            int lineOrder
    ) {
        return BoothQueueLine.create(
                boothId,
                lineOrder,
                BoothLineLabel.of("라인 " + lineOrder),
                lineOrder * 10,
                100,
                "{}",
                "{}"
        );
    }
}
