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
    @DisplayName("searchCandidates")
    class SearchCandidates {

        @Test
        @DisplayName("검색어를 정리해서 후보자를 검색한다")
        void success_SearchCandidates() {
            // given
            Long festivalId = 1L;
            AdminSubAdminCandidateView view = candidateView();
            given(queryRepository.searchCandidates(festivalId, "mapo"))
                    .willReturn(List.of(view));

            // when
            List<AdminSubAdminCandidateView> result =
                    queryService.searchCandidates(festivalId, " MAPO ");

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("빈 검색어는 전체 조회로 처리한다")
        void success_SearchCandidates_BlankKeywordBoundary() {
            // given
            Long festivalId = 1L;
            AdminSubAdminCandidateView view = candidateView();
            given(queryRepository.searchCandidates(festivalId, null))
                    .willReturn(List.of(view));

            // when
            List<AdminSubAdminCandidateView> result =
                    queryService.searchCandidates(festivalId, " ");

            // then
            assertThat(result).containsExactly(view);
        }
    }

    private AdminSubAdminCandidateView candidateView() {
        return new AdminSubAdminCandidateView(
                UUID.randomUUID(),
                "candidate@mapo.go.kr",
                "김후보",
                "마포구청 소속",
                AdminStatus.ACTIVE
        );
    }
}
