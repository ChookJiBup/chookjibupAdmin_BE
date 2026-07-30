package com.example.demoadmin.booth.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.FestivalBoothRepository;
import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FestivalBoothRepositoryTest {

    @Autowired
    private FestivalBoothRepository festivalBoothRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("축제 부스를 저장한다")
        void success_Save() {
            // given
            FestivalBooth booth = booth(1L);

            // when
            FestivalBooth saved = festivalBoothRepository.save(booth);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getNameValue()).isEqualTo("푸드 부스");
        }
    }

    @Nested
    @DisplayName("findByFestivalIdAndPublicIdForUpdate")
    class FindByFestivalIdAndPublicIdForUpdate {

        @Test
        @DisplayName("수정 잠금으로 축제 부스를 조회한다")
        void success_FindByFestivalIdAndPublicIdForUpdate() {
            // given
            FestivalBooth saved = festivalBoothRepository.save(booth(1L));

            // when
            var found = festivalBoothRepository.findByFestivalIdAndPublicIdForUpdate(
                    1L,
                    saved.getPublicId()
            );

            // then
            assertThat(found).contains(saved);
        }
    }

    @Nested
    @DisplayName("findByFestivalIdAndPublicId")
    class FindByFestivalIdAndPublicId {

        @Test
        @DisplayName("축제 ID와 부스 UUID로 부스를 조회한다")
        void success_FindByFestivalIdAndPublicId() {
            // given
            FestivalBooth saved = festivalBoothRepository.save(booth(1L));

            // when
            var found = festivalBoothRepository.findByFestivalIdAndPublicId(
                    1L,
                    saved.getPublicId()
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(FestivalBooth::getId)
                    .isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("다른 축제 ID로는 부스를 조회하지 않는다")
        void success_FindByFestivalIdAndPublicId_FestivalBoundary() {
            // given
            FestivalBooth saved = festivalBoothRepository.save(booth(1L));

            // when
            var found = festivalBoothRepository.findByFestivalIdAndPublicId(
                    2L,
                    saved.getPublicId()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    private FestivalBooth booth(Long festivalId) {
        return FestivalBooth.create(
                festivalId,
                BoothName.of("푸드 부스"),
                BoothCategory.of("먹거리"),
                BoothLocation.of("A-1"),
                BoothDescription.of("대표 먹거리 부스")
        );
    }
}
