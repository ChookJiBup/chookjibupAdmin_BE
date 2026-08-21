package com.example.chookjibupadmin.report.command.infrastructure.persistence;

import com.example.chookjibupadmin.report.command.domain.FestivalResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalResultJpaRepository
        extends JpaRepository<FestivalResult, Long> {
}
