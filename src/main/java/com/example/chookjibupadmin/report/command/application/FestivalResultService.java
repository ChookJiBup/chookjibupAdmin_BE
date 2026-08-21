package com.example.chookjibupadmin.report.command.application;

import com.example.chookjibupadmin.report.command.domain.FestivalResult;
import com.example.chookjibupadmin.report.command.domain.FestivalResultRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 결과 보고서 스냅샷 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalResultService {

    private final FestivalResultRepository repository;

    @Transactional
    public FestivalResult save(FestivalResult result) {
        return repository.save(result);
    }

    public Optional<FestivalResult> findByFestivalId(Long festivalId) {
        return repository.findByFestivalId(festivalId);
    }
}
