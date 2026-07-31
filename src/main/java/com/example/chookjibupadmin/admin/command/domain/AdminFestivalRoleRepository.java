package com.example.chookjibupadmin.admin.command.domain;

import java.util.Optional;

/**
 * 축제별 관리자 역할 저장소 계약이다.
 */
public interface AdminFestivalRoleRepository {

    AdminFestivalRole save(AdminFestivalRole adminFestivalRole);

    Optional<AdminFestivalRole> findByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    );

    boolean existsByFestivalIdAndRole(Long festivalId, AdminRole role);

    boolean existsByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    );
}
