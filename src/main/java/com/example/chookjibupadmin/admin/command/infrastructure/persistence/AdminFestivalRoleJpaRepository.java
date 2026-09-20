package com.example.chookjibupadmin.admin.command.infrastructure.persistence;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AdminFestivalRoleJpaRepository
        extends JpaRepository<AdminFestivalRole, Long> {

    Optional<AdminFestivalRole> findByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    );

    boolean existsByFestivalIdAndRole(Long festivalId, AdminRole role);

    boolean existsByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    );

    boolean existsByAdminAccountIdAndRole(Long adminAccountId, AdminRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AdminFestivalRole> findAllByAdminAccountIdInAndFestivalId(
            Collection<Long> adminAccountIds,
            Long festivalId
    );

    List<AdminFestivalRole> findAllByFestivalId(Long festivalId);

    /**
     * 계정이 가진 축제 역할 종류만 한 번에 읽는다.
     *
     * <p>역할 엔티티를 축제 수만큼 적재하지 않으려고 role 컬럼만 distinct로 뽑는다.
     * 총괄 10곳·운영자 3곳인 계정이라도 결과는 두 줄이다.
     * (admin_account_id, festival_id) 유니크 인덱스의 선두 컬럼으로 좁혀진다.</p>
     */
    @Query("SELECT DISTINCT r.role FROM AdminFestivalRole r "
            + "WHERE r.adminAccountId = :adminAccountId")
    List<AdminRole> findDistinctRolesByAdminAccountId(
            @Param("adminAccountId") Long adminAccountId
    );
}
