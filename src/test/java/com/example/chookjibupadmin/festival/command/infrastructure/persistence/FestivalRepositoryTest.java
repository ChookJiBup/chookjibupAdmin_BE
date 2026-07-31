package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalRepository;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
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
class FestivalRepositoryTest {

    @Autowired
    private FestivalRepository festivalRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("축제 기본 정보를 저장한다")
        void success_Save() {
            // given
            Festival festival = festival();

            // when
            Festival saved = festivalRepository.save(festival);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getNameValue()).isEqualTo("마포나루 새우젓축제");
        assertThat(saved.getDetailAddressValue()).isEqualTo("월드컵공원");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("축제 ID로 축제 기본 정보를 조회한다")
        void success_FindById() {
            // given
            Festival saved = festivalRepository.save(festival());

            // when
            var found = festivalRepository.findById(saved.getId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(Festival::getNameValue)
                    .isEqualTo("마포나루 새우젓축제");
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("외부 노출용 축제 UUID로 축제 기본 정보를 조회한다")
        void success_FindByPublicId() {
            // given
            Festival saved = festivalRepository.save(festival());

            // when
            var found = festivalRepository.findByPublicId(saved.getPublicId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(Festival::getId)
                    .isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("일치하는 축제 UUID가 없으면 빈 결과를 반환한다")
        void success_FindByPublicId_NotFoundBoundary() {
            // given
            festivalRepository.save(festival());

            // when
            var found = festivalRepository.findByPublicId(
                    java.util.UUID.randomUUID()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsBySeriesIdAndYear")
    class ExistsBySeriesIdAndYear {

        @Test
        @DisplayName("축제 묶음의 개최 연도 존재 여부를 확인한다")
        void success_ExistsBySeriesIdAndYear() {
            // given
            festivalRepository.save(festival());

            // when
            boolean exists = festivalRepository.existsBySeriesIdAndYear(
                    1L,
                    2026
            );

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("없는 개최 연도는 false를 반환한다")
        void success_ExistsBySeriesIdAndYear_NotFoundBoundary() {
            // given
            festivalRepository.save(festival());

            // when
            boolean exists = festivalRepository.existsBySeriesIdAndYear(
                    1L,
                    2025
            );

            // then
            assertThat(exists).isFalse();
        }
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
