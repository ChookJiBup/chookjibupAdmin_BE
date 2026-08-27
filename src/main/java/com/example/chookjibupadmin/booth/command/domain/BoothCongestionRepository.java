package com.example.chookjibupadmin.booth.command.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoothCongestionRepository {

    BoothCongestion save(BoothCongestion congestion);

    Optional<BoothCongestion> findLatestByBoothId(Long boothId);

    List<BoothCongestion> findLatestByBoothIds(Collection<Long> boothIds);

    long countDistinctBoothsWithCongestion(Long festivalId);
}
