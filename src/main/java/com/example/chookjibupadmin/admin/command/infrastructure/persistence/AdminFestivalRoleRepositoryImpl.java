package com.example.chookjibupadmin.admin.command.infrastructure.persistence;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRoleRepository;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import java.util.Collection;
import java.util.List;
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

    @Override
    public boolean existsByAdminAccountIdAndRole(Long adminAccountId, AdminRole role) {
        return jpaRepository.existsByAdminAccountIdAndRole(adminAccountId, role);
    }

    @Override
    public List<AdminFestivalRole> findAllByAdminAccountIdInAndFestivalId(
            Collection<Long> adminAccountIds,
            Long festivalId
    ) {
        return jpaRepository.findAllByAdminAccountIdInAndFestivalId(
                adminAccountIds,
                festivalId
        );
    }

    @Override
    public List<AdminFestivalRole> findAllByFestivalId(Long festivalId) {
        return jpaRepository.findAllByFestivalId(festivalId);
    }

    @Override
    public void deleteAll(Collection<AdminFestivalRole> roles) {
        jpaRepository.deleteAllInBatch(roles);
    }
}
