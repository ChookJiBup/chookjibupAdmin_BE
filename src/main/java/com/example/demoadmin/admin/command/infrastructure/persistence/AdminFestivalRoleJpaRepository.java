package com.example.demoadmin.admin.command.infrastructure.persistence;

import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.admin.command.domain.AdminRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
