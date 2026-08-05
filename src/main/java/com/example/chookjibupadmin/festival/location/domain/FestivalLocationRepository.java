package com.example.chookjibupadmin.festival.location.domain;

import java.util.List;

public interface FestivalLocationRepository {
    List<FestivalLocation> saveAll(Iterable<FestivalLocation> locations);

    List<FestivalLocation> findAllByFestivalId(Long festivalId);

    void deleteAll(Iterable<FestivalLocation> locations);
}
