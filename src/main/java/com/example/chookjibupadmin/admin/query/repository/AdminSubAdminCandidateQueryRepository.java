package com.example.chookjibupadmin.admin.query.repository;

import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import java.util.List;

/**
 * 서브관리자 초대 후보 조회 저장소 계약이다.
 */
public interface AdminSubAdminCandidateQueryRepository {

    /**
     * 해당 축제에 아직 배정되지 않은 활성 관리자 계정을 조회한다.
     */
    List<AdminSubAdminCandidateView> findCandidates(Long festivalId);
}
