package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothInfoRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoothInfoRepositoryImpl implements BoothInfoRepository {

    private final BoothInfoJpaRepository jpaRepository;

    @Override
    public BoothInfo save(BoothInfo boothInfo) {
        return jpaRepository.save(boothInfo);
    }

    @Override
    public Optional<BoothInfo> findById(Long boothId) {
        return jpaRepository.findById(boothId);
    }

    @Override
    public Optional<BoothInfo> findByFestivalIdAndRoadmapNodeId(
            Long festivalId,
            Long roadmapNodeId
    ) {
        return jpaRepository.findByFestivalIdAndRoadmapNodeId(
                festivalId,
                roadmapNodeId
        );
    }

    @Override
    public List<BoothInfo> findAllByFestivalId(Long festivalId) {
        return jpaRepository.findAllByFestivalIdOrderByIdAsc(festivalId);
    }

    @Override
    public long countByFestivalId(Long festivalId) {
        return jpaRepository.countByFestivalId(festivalId);
    }
}
