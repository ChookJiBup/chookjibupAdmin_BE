package com.example.chookjibupadmin.map.roadmap.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FestivalRoadmapTest {

    @Test
    @DisplayName("부스 구역을 안정 식별자와 멤버십으로 교체한다")
    void success_ReplaceZones() {
        FestivalRoadmap roadmap = FestivalRoadmap.createForCoordinateMap(1L, 2L, 3L);
        UUID zoneId = UUID.randomUUID();
        UUID boothId = UUID.randomUUID();

        roadmap.replaceZones(List.of(new RoadmapZone(zoneId, "이벤트 구역", 0, List.of(boothId))));

        assertThat(roadmap.getZones()).containsExactly(
                new RoadmapZone(zoneId, "이벤트 구역", 0, List.of(boothId)));
        assertThatThrownBy(() -> roadmap.getZones().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

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
    @DisplayName("분석 중에는 관리자 편집을 거부한다")
    void fail_ApplyAdminEdit_WhileAnalyzing() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);

        assertThatThrownBy(() -> roadmap.applyAdminEdit(0L))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FESTIVAL_MAP_INVALID_STATUS)
                );
    }

    @Test
    @DisplayName("분석이 중단되면 편집 상태로 되돌리고 리비전은 유지한다")
    void success_AnalysisAborted() {
        // given
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);

        // when
        roadmap.analysisAborted();

        // then
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.EDITING);
        assertThat(roadmap.getEditRevision()).isZero();
    }

    @Test
    @DisplayName("분석이 중단된 뒤에는 관리자가 부스를 저장할 수 있다")
    void success_ApplyAdminEdit_AfterAnalysisAborted() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        roadmap.analysisAborted();

        long revision = roadmap.applyAdminEdit(0L);

        assertThat(revision).isEqualTo(1L);
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.EDITING);
    }

    @Test
    @DisplayName("분석 중이 아니면 중단 처리가 상태를 바꾸지 않는다")
    void success_AnalysisAborted_KeepsNonAnalyzingStatus() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        roadmap.analysisCompleted();

        roadmap.analysisAborted();

        assertThat(roadmap.getStatus())
                .isEqualTo(RoadmapStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("공개하면 PUBLISHED가 되고 공개 버전이 현재 리비전을 따른다")
    void success_Publish() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        roadmap.analysisCompleted();
        roadmap.applyAdminEdit(1L);

        roadmap.publish();

        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.PUBLISHED);
        assertThat(roadmap.isPublished()).isTrue();
        assertThat(roadmap.getPublishedVersion())
                .isEqualTo(roadmap.getEditRevision());
    }

    @Test
    @DisplayName("분석 중에는 공개를 거부한다")
    void fail_Publish_WhileAnalyzing() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);

        assertThatThrownBy(roadmap::publish)
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FESTIVAL_MAP_INVALID_STATUS)
                );
        assertThat(roadmap.isPublished()).isFalse();
    }

    @Test
    @DisplayName("공개한 뒤 편집을 저장해도 공개가 유지되고 공개 버전이 따라온다")
    void success_ApplyAdminEdit_AfterPublish_StaysPublished() {
        FestivalRoadmap roadmap = FestivalRoadmap.createForCoordinateMap(1L, 10L, 2L);
        roadmap.publish();

        long revision = roadmap.applyAdminEdit(0L);

        // 저장할 때마다 감춰지면 관리자는 부스맵이 사라진 줄도 모르고 넘어간다.
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.PUBLISHED);
        assertThat(roadmap.isPublished()).isTrue();
        assertThat(roadmap.getPublishedVersion()).isEqualTo(revision);
    }

    @Test
    @DisplayName("공개를 해제하면 편집 상태로 돌아가고 공개 버전을 지운다")
    void success_Unpublish() {
        FestivalRoadmap roadmap = FestivalRoadmap.createForCoordinateMap(1L, 10L, 2L);
        roadmap.applyAdminEdit(0L);
        roadmap.publish();

        roadmap.unpublish();

        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.EDITING);
        assertThat(roadmap.isPublished()).isFalse();
        assertThat(roadmap.getPublishedVersion()).isZero();
        // 그려 둔 내용은 그대로 두므로 편집 리비전은 건드리지 않는다.
        assertThat(roadmap.getEditRevision()).isEqualTo(1L);
    }

    @Test
    @DisplayName("공개 중이 아니면 공개 해제가 아무것도 바꾸지 않는다")
    void success_Unpublish_WhenNotPublished() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        roadmap.analysisCompleted();

        roadmap.unpublish();

        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.REVIEW_REQUIRED);
        assertThat(roadmap.getEditRevision()).isEqualTo(1L);
        assertThat(roadmap.getPublishedVersion()).isZero();
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
