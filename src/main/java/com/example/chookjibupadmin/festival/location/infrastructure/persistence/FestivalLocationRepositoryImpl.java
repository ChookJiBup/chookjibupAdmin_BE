package com.example.chookjibupadmin.festival.location.infrastructure.persistence;

import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalLocationRepositoryImpl implements FestivalLocationRepository {

    private final FestivalLocationJpaRepository jpaRepository;

    @Override
    public List<FestivalLocation> saveAll(Iterable<FestivalLocation> locations) {
        return jpaRepository.saveAll(locations);
    }

    @Override
    public List<FestivalLocation> findAllByFestivalId(Long festivalId) {
        return jpaRepository.findAllByFestival_IdOrderBySortOrderAscIdAsc(festivalId);
    }

    @Override
    public void deleteAll(Iterable<FestivalLocation> locations) {
        jpaRepository.deleteAll(locations);
    }
}
