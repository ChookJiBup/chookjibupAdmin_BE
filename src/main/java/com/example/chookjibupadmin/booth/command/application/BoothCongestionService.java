package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부스 혼잡 이력 저장소 래퍼이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothCongestionService {

    private final BoothCongestionRepository boothCongestionRepository;

    @Transactional
    public BoothCongestion save(BoothCongestion congestion) {
        return boothCongestionRepository.save(congestion);
    }

    public Optional<BoothCongestion> findLatestByBoothId(Long boothId) {
        return boothCongestionRepository.findLatestByBoothId(boothId);
    }

    public List<BoothCongestion> findLatestByBoothIds(Collection<Long> boothIds) {
        return boothCongestionRepository.findLatestByBoothIds(boothIds);
    }

    public long countDistinctBoothsWithCongestion(Long festivalId) {
        return boothCongestionRepository.countDistinctBoothsWithCongestion(festivalId);
    }
}
