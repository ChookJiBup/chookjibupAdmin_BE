package com.example.chookjibupadmin.festival.location.infrastructure.persistence;

import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalLocationJpaRepository extends JpaRepository<FestivalLocation, Long> {
    List<FestivalLocation> findAllByFestival_IdOrderBySortOrderAscIdAsc(Long festivalId);
}
