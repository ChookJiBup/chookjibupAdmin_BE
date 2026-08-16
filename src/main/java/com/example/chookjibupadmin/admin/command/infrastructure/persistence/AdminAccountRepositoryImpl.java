package com.example.chookjibupadmin.admin.command.infrastructure.persistence;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminAccountRepository;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminAccountRepositoryImpl implements AdminAccountRepository {

    private final AdminAccountJpaRepository jpaRepository;

    @Override
    public AdminAccount save(AdminAccount adminAccount) {
        return jpaRepository.save(adminAccount);
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
    public List<AdminAccount> findAllByPublicIdIn(Collection<UUID> publicIds) {
        return jpaRepository.findAllByPublicIdIn(publicIds);
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
