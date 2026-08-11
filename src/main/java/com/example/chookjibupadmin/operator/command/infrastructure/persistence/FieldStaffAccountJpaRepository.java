package com.example.chookjibupadmin.operator.command.infrastructure.persistence;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FieldStaffAccountJpaRepository extends JpaRepository<FieldStaffAccount, Long> {

    Optional<FieldStaffAccount> findByPublicId(UUID publicId);

    List<FieldStaffAccount> findAllByPublicIdIn(Collection<UUID> publicIds);

    Optional<FieldStaffAccount> findByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    );

    boolean existsByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    );

    void deleteAllByFestivalId(Long festivalId);
}
