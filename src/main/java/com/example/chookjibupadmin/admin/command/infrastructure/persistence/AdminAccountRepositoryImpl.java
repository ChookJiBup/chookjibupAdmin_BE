package com.example.chookjibupadmin.admin.command.infrastructure.persistence;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminAccountRepository;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminAccountRepositoryImpl implements AdminAccountRepository {

    private final AdminAccountJpaRepository jpaRepository;
    private final AdminFestivalRoleJpaRepository adminFestivalRoleJpaRepository;

    @Override
    public AdminAccount save(AdminAccount adminAccount) {
        AdminAccount saved = jpaRepository.save(adminAccount);
        saveDeprecatedFixtureRole(saved);
        return saved;
    }

    private void saveDeprecatedFixtureRole(AdminAccount adminAccount) {
        if (adminAccount.getFestivalId() == null || adminAccount.getRole() == null) {
            return;
        }
        if (adminFestivalRoleJpaRepository.existsByAdminAccountIdAndFestivalId(
                adminAccount.getId(),
                adminAccount.getFestivalId()
        )) {
            return;
        }

        if (adminAccount.getRole() == AdminRole.FESTIVAL_OWNER) {
            adminFestivalRoleJpaRepository.save(AdminFestivalRole.createFestivalOwner(
                    adminAccount.getId(),
                    adminAccount.getFestivalId()
            ));
            return;
        }

        if (adminAccount.getRole() == AdminRole.SUB_ADMIN) {
            adminFestivalRoleJpaRepository.save(AdminFestivalRole.createSubAdmin(
                    adminAccount.getId(),
                    adminAccount.getFestivalId(),
                    adminAccount.getInvitedByAdminId()
            ));
        }
    }

    @Override
    public Optional<AdminAccount> findById(Long adminAccountId) {
        return jpaRepository.findById(adminAccountId);
    }

    @Override
    public Optional<AdminAccount> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public boolean existsByEmail(AdminEmail email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<AdminAccount> findByEmail(AdminEmail email) {
        return jpaRepository.findByEmail(email);
    }
}
