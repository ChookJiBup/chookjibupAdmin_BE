package com.example.demoadmin.map.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.domain.MapAnalysisJob;
import com.example.demoadmin.map.command.domain.MapAnalysisJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapAnalysisJobRepositoryTest {

    @Autowired
    private MapAnalysisJobRepository mapAnalysisJobRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("배치도 분석 작업을 저장한다")
        void success_Save() {
            // given
            MapAnalysisJob analysisJob = MapAnalysisJob.create(1L, 1L);

            // when
            MapAnalysisJob saved = mapAnalysisJobRepository.save(analysisJob);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("분석 작업 UUID로 작업을 조회한다")
        void success_FindByPublicId() {
            // given
            MapAnalysisJob saved = mapAnalysisJobRepository.save(
                    MapAnalysisJob.create(1L, 1L)
            );

            // when
            var found = mapAnalysisJobRepository.findByPublicId(saved.getPublicId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(MapAnalysisJob::getId)
                    .isEqualTo(saved.getId());
        }
    }
}
