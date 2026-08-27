package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoothCongestionRepositoryImpl implements BoothCongestionRepository {

    private final BoothCongestionJpaRepository jpaRepository;

    @Override
    public BoothCongestion save(BoothCongestion congestion) {
        return jpaRepository.save(congestion);
    }

    @Override
    public Optional<BoothCongestion> findLatestByBoothId(Long boothId) {
        return jpaRepository.findLatestByBoothId(boothId);
    }

    @Override
    public List<BoothCongestion> findLatestByBoothIds(Collection<Long> boothIds) {
        if (boothIds == null || boothIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findLatestByBoothIds(boothIds);
    }

    @Override
    public long countDistinctBoothsWithCongestion(Long festivalId) {
        return jpaRepository.countDistinctBoothsWithCongestion(festivalId);
    }
}
