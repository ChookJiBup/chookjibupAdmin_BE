package com.example.demoadmin.booth.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.BoothQueueLineRepository;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BoothQueueLineRepositoryTest {

    @Autowired
    private BoothQueueLineRepository boothQueueLineRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("부스 대기 라인을 저장한다")
        void success_Save() {
            // given
            BoothQueueLine queueLine = queueLine(1L, 1);

            // when
            BoothQueueLine saved = boothQueueLineRepository.save(queueLine);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getLineOrder()).isOne();
        }
    }

    @Nested
    @DisplayName("findByBoothIdAndPublicId")
    class FindByBoothIdAndPublicId {

        @Test
        @DisplayName("부스 ID와 대기 라인 UUID로 대기 라인을 조회한다")
        void success_FindByBoothIdAndPublicId() {
            // given
            BoothQueueLine saved = boothQueueLineRepository.save(queueLine(1L, 1));

            // when
            var found = boothQueueLineRepository.findByBoothIdAndPublicId(
                    1L,
                    saved.getPublicId()
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(BoothQueueLine::getId)
                    .isEqualTo(saved.getId());
        }
    }

    @Nested
    @DisplayName("existsByBoothIdAndLineOrder")
    class ExistsByBoothIdAndLineOrder {

        @Test
        @DisplayName("같은 부스의 대기 라인 순서 존재 여부를 확인한다")
        void success_ExistsByBoothIdAndLineOrder() {
            // given
            boothQueueLineRepository.save(queueLine(1L, 1));

            // when
            boolean exists = boothQueueLineRepository.existsByBoothIdAndLineOrder(1L, 1);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("다른 부스의 같은 순서는 중복으로 보지 않는다")
        void success_ExistsByBoothIdAndLineOrder_BoothBoundary() {
            // given
            boothQueueLineRepository.save(queueLine(1L, 1));

            // when
            boolean exists = boothQueueLineRepository.existsByBoothIdAndLineOrder(2L, 1);

            // then
            assertThat(exists).isFalse();
        }
    }

    private BoothQueueLine queueLine(
            Long boothId,
            int lineOrder
    ) {
        return BoothQueueLine.create(
                boothId,
                lineOrder,
                BoothLineLabel.of("라인 " + lineOrder),
                10,
                100,
                "{}",
                "{}"
        );
    }
}
