package com.example.chookjibupadmin.map.roadmap.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FestivalRoadmapTest {

    @Test
    @DisplayName("좌표 전용 지도 로드맵은 바로 편집 상태로 생성한다")
    void success_CreateForCoordinateMap() {
        FestivalRoadmap roadmap = FestivalRoadmap.createForCoordinateMap(1L, 10L, 2L);

        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.EDITING);
        assertThat(roadmap.getCurrentMapId()).isEqualTo(10L);
        assertThat(roadmap.getEditRevision()).isZero();
    }

    @Test
    @DisplayName("최초 도면 분석을 기다리는 축제 로드맵을 생성한다")
    void success_Create() {
        // when
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);

        // then
        assertThat(roadmap.getPublicId()).isNotNull();
        assertThat(roadmap.getFestivalId()).isEqualTo(1L);
        assertThat(roadmap.getCurrentMapId()).isEqualTo(10L);
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.ANALYZING);
        assertThat(roadmap.getEditRevision()).isZero();
    }

    @Test
    @DisplayName("도면을 교체하면 분석 상태로 되돌리고 편집 리비전을 증가시킨다")
    void success_ReplaceMap() {
        // given
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);

        // when
        roadmap.replaceMap(11L);

        // then
        assertThat(roadmap.getCurrentMapId()).isEqualTo(11L);
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.ANALYZING);
        assertThat(roadmap.getEditRevision()).isEqualTo(1L);
    }

    @Test
    @DisplayName("분석이 완료되면 관리자 검수 대기 상태로 전환한다")
    void success_AnalysisCompleted() {
        // given
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);

        // when
        roadmap.analysisCompleted();

        // then
        assertThat(roadmap.getStatus())
                .isEqualTo(RoadmapStatus.REVIEW_REQUIRED);
        assertThat(roadmap.getEditRevision()).isEqualTo(1L);
    }

    @Test
    @DisplayName("현재 리비전으로 편집하면 편집 상태로 전환하고 리비전을 증가시킨다")
    void success_ApplyAdminEdit() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        roadmap.analysisCompleted();

        long revision = roadmap.applyAdminEdit(1L);

        assertThat(revision).isEqualTo(2L);
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.EDITING);
    }

    @Test
    @DisplayName("오래된 리비전으로 편집하면 충돌 예외를 던진다")
    void fail_ApplyAdminEdit_RevisionConflict() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        roadmap.analysisCompleted();

        assertThatThrownBy(() -> roadmap.applyAdminEdit(0L))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.ROADMAP_REVISION_CONFLICT)
                );
    }
}
