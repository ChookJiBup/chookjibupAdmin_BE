package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminCandidateQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서브관리자 초대 후보 조회 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSubAdminCandidateQueryService {

    private final AdminSubAdminCandidateQueryRepository queryRepository;

    /**
     * 초대 가능한 활성 관리자 계정을 검색한다.
     */
    public List<AdminSubAdminCandidateView> searchCandidates(
            Long festivalId,
            String keyword
    ) {
        return queryRepository.searchCandidates(festivalId, normalizeKeyword(keyword));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim().toLowerCase();
    }
}
