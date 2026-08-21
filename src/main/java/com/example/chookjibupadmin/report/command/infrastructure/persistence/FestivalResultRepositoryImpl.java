package com.example.chookjibupadmin.report.command.infrastructure.persistence;

import com.example.chookjibupadmin.report.command.domain.FestivalResult;
import com.example.chookjibupadmin.report.command.domain.FestivalResultRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalResultRepositoryImpl implements FestivalResultRepository {

    private final FestivalResultJpaRepository jpaRepository;

    @Override
    public FestivalResult save(FestivalResult result) {
        return jpaRepository.save(result);
    }

    @Override
    public Optional<FestivalResult> findByFestivalId(Long festivalId) {
        return jpaRepository.findById(festivalId);
    }
}
