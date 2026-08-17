package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminCandidateQueryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSubAdminCandidateQueryServiceTest {

    @InjectMocks
    private AdminSubAdminCandidateQueryService queryService;

    @Mock
    private AdminSubAdminCandidateQueryRepository queryRepository;

    @Nested
    @DisplayName("findCandidates")
    class FindCandidates {

        @Test
        @DisplayName("축제에 초대 가능한 후보자를 조회한다")
        void success_FindCandidates() {
            // given
            Long festivalId = 1L;
            AdminSubAdminCandidateView view = candidateView();
            given(queryRepository.findCandidates(festivalId))
                    .willReturn(List.of(view));

            // when
            List<AdminSubAdminCandidateView> result =
                    queryService.findCandidates(festivalId);

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("후보자가 없으면 빈 목록을 반환한다")
        void success_FindCandidates_EmptyBoundary() {
            // given
            Long festivalId = 1L;
            given(queryRepository.findCandidates(festivalId))
                    .willReturn(List.of());

            // when
            List<AdminSubAdminCandidateView> result =
                    queryService.findCandidates(festivalId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private AdminSubAdminCandidateView candidateView() {
        return new AdminSubAdminCandidateView(
                UUID.randomUUID(),
                "candidate@mapo.go.kr",
                "김후보",
                "관광정책과",
                "주무관",
                AdminStatus.ACTIVE
        );
    }
}
