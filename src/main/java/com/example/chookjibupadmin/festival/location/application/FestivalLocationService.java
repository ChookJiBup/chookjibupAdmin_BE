package com.example.chookjibupadmin.festival.location.application;

import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalLocationService {

    private final FestivalLocationRepository repository;

    public List<FestivalLocation> findAllByFestivalId(Long festivalId) {
        return repository.findAllByFestivalId(festivalId);
    }

    @Transactional
    public List<FestivalLocation> saveAll(List<FestivalLocation> locations) {
        return repository.saveAll(locations);
    }

    @Transactional
    public void deleteAll(List<FestivalLocation> locations) {
        repository.deleteAll(locations);
    }
}
