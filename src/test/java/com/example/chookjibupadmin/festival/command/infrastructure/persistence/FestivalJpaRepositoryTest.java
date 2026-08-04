package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class FestivalJpaRepositoryTest {

    @Autowired
    private FestivalJpaRepository festivalJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("축제 기본 정보를 DB에 저장한다")
        void success_Save() {
            // given
            Festival festival = festival();

            // when
            Festival saved = festivalJpaRepository.saveAndFlush(festival);
            entityManager.clear();
            Festival found = festivalJpaRepository.findById(saved.getId())
                    .orElseThrow();

            // then
            assertThat(found.getId()).isNotNull();
            assertThat(found.getPublicId()).isNotNull();
            assertThat(found.getNameValue()).isEqualTo("마포나루 새우젓축제");
            assertThat(found.getStatus()).isEqualTo(FestivalStatus.DRAFT);
        }

        @Test
        @DisplayName("최소 축제 기간 경계값으로 DB에 저장한다")
        void success_Save_SameDateBoundary() {
            // given
            LocalDate date = LocalDate.of(2026, 10, 16);
            Festival festival = festival(date, date);

            // when
            Festival saved = festivalJpaRepository.saveAndFlush(festival);
            entityManager.clear();
            Festival found = festivalJpaRepository.findById(saved.getId())
                    .orElseThrow();

            // then
            assertThat(found.getStartDate()).isEqualTo(date);
            assertThat(found.getEndDate()).isEqualTo(date);
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("외부 노출용 축제 UUID로 DB에서 조회한다")
        void success_FindByPublicId() {
            // given
            Festival saved = festivalJpaRepository.saveAndFlush(festival());
            entityManager.clear();

            // when
            var found = festivalJpaRepository.findByPublicId(saved.getPublicId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(Festival::getDetailAddressValue)
                    .isEqualTo("월드컵공원");
        }
    }

    @Nested
    @DisplayName("findByIdForUpdate")
    class FindByIdForUpdate {

        @Test
        @DisplayName("축제 ID로 교체 직렬화용 잠금 조회를 수행한다")
        void success_FindByIdForUpdate() {
            Festival saved = festivalJpaRepository.saveAndFlush(festival());
            entityManager.clear();

            var found = festivalJpaRepository.findByIdForUpdate(saved.getId());

            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(Festival::getPublicId)
                    .isEqualTo(saved.getPublicId());
        }
    }

    @Nested
    @DisplayName("existsBySeriesIdAndYear")
    class ExistsBySeriesIdAndYear {

        @Test
        @DisplayName("축제 묶음과 개최 연도로 DB에서 존재 여부를 확인한다")
        void success_ExistsBySeriesIdAndYear() {
            // given
            festivalJpaRepository.saveAndFlush(festival());
            entityManager.clear();

            // when
            boolean exists = festivalJpaRepository.existsBySeriesIdAndYear(
                    1L,
                    2026
            );

            // then
            assertThat(exists).isTrue();
        }
    }

    private Festival festival() {
        return festival(
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18)
        );
    }

    private Festival festival(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(startDate, endDate),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
