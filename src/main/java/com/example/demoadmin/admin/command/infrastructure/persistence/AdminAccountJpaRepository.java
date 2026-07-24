package com.example.demoadmin.admin.command.infrastructure.persistence;

import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.domain.vo.AdminEmail;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminAccountJpaRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByPublicId(UUID publicId);

    boolean existsByEmail(AdminEmail email);

    Optional<AdminAccount> findByEmail(AdminEmail email);
}
