package com.example.demoadmin.map.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.domain.FestivalMap;
import com.example.demoadmin.map.command.domain.FestivalMapRepository;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.example.demoadmin.map.command.domain.vo.MapFileName;
import com.example.demoadmin.map.command.domain.vo.MapStoragePath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FestivalMapRepositoryTest {

    @Autowired
    private FestivalMapRepository festivalMapRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("축제 배치도를 저장한다")
        void success_Save() {
            // given
            FestivalMap festivalMap = festivalMap(1L);

            // when
            FestivalMap saved = festivalMapRepository.save(festivalMap);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getOriginalFileNameValue()).isEqualTo("김밥축제_지적편집도.png");
        }
    }

    @Nested
    @DisplayName("findByFestivalIdAndPublicId")
    class FindByFestivalIdAndPublicId {

        @Test
        @DisplayName("축제 ID와 배치도 UUID로 배치도를 조회한다")
        void success_FindByFestivalIdAndPublicId() {
            // given
            FestivalMap saved = festivalMapRepository.save(festivalMap(1L));

            // when
            var found = festivalMapRepository.findByFestivalIdAndPublicId(
                    1L,
                    saved.getPublicId()
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(FestivalMap::getId)
                    .isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("다른 축제 ID로는 배치도를 조회하지 않는다")
        void success_FindByFestivalIdAndPublicId_FestivalBoundary() {
            // given
            FestivalMap saved = festivalMapRepository.save(festivalMap(1L));

            // when
            var found = festivalMapRepository.findByFestivalIdAndPublicId(
                    2L,
                    saved.getPublicId()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    private FestivalMap festivalMap(Long festivalId) {
        return FestivalMap.create(
                festivalId,
                MapFileName.of("김밥축제_지적편집도.png"),
                MapStorageType.TEST_RESOURCE,
                MapStoragePath.of("images/김밥축제_지적편집도.png"),
                1745,
                1577
        );
    }
}
