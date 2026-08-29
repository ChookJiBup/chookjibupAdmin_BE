package com.example.chookjibupadmin.operator.command.infrastructure.persistence;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccountRepository;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FieldStaffAccountRepositoryImpl implements FieldStaffAccountRepository {

    private final FieldStaffAccountJpaRepository jpaRepository;

    @Override
    public FieldStaffAccount save(FieldStaffAccount fieldStaffAccount) {
        return jpaRepository.save(fieldStaffAccount);
    }

    @Override
    public Optional<FieldStaffAccount> findById(Long fieldStaffAccountId) {
        return jpaRepository.findById(fieldStaffAccountId);
    }

    @Override
    public List<FieldStaffAccount> findAllById(Collection<Long> fieldStaffAccountIds) {
        if (fieldStaffAccountIds == null || fieldStaffAccountIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(fieldStaffAccountIds);
    }

    @Override
    public Optional<FieldStaffAccount> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public List<FieldStaffAccount> findAllByPublicIdIn(
            Collection<UUID> publicIds
    ) {
        return jpaRepository.findAllByPublicIdIn(publicIds);
    }

    @Override
    public Optional<FieldStaffAccount> findByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    ) {
        return jpaRepository.findByFestivalIdAndLoginId(festivalId, loginId);
    }

    @Override
    public boolean existsByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    ) {
        return jpaRepository.existsByFestivalIdAndLoginId(festivalId, loginId);
    }

    @Override
    public void deleteAllByFestivalId(Long festivalId) {
        jpaRepository.deleteAllByFestivalId(festivalId);
    }
}
