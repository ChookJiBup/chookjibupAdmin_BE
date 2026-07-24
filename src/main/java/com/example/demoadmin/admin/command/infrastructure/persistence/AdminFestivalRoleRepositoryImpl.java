package com.example.demoadmin.admin.command.infrastructure.persistence;

import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.admin.command.domain.AdminFestivalRoleRepository;
import com.example.demoadmin.admin.command.domain.AdminRole;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminFestivalRoleRepositoryImpl
        implements AdminFestivalRoleRepository {

    private final AdminFestivalRoleJpaRepository jpaRepository;

    @Override
    public AdminFestivalRole save(AdminFestivalRole adminFestivalRole) {
        return jpaRepository.save(adminFestivalRole);
    }

    @Override
    public Optional<AdminFestivalRole> findByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    ) {
        return jpaRepository.findByAdminAccountIdAndFestivalId(
                adminAccountId,
                festivalId
        );
    }

    @Override
    public boolean existsByFestivalIdAndRole(Long festivalId, AdminRole role) {
        return jpaRepository.existsByFestivalIdAndRole(festivalId, role);
    }

    @Override
    public boolean existsByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    ) {
        return jpaRepository.existsByAdminAccountIdAndFestivalId(
                adminAccountId,
                festivalId
        );
    }
}
