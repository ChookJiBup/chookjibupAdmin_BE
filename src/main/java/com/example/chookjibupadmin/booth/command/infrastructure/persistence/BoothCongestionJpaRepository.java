package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoothCongestionJpaRepository extends JpaRepository<BoothCongestion, Long> {

    @Query(
            value = """
                    SELECT * FROM booth_congestion
                    WHERE booth_id = :boothId
                    ORDER BY created_at DESC, congestion_id DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<BoothCongestion> findLatestByBoothId(@Param("boothId") Long boothId);

    @Query(
            value = """
                    SELECT bc.*
                    FROM booth_congestion bc
                    INNER JOIN (
                        SELECT booth_id,
                               MAX(created_at) AS max_created
                        FROM booth_congestion
                        WHERE booth_id IN (:boothIds)
                        GROUP BY booth_id
                    ) latest
                      ON bc.booth_id = latest.booth_id
                     AND bc.created_at = latest.max_created
                    INNER JOIN (
                        SELECT booth_id,
                               created_at,
                               MAX(congestion_id) AS max_id
                        FROM booth_congestion
                        WHERE booth_id IN (:boothIds)
                        GROUP BY booth_id, created_at
                    ) tie
                      ON bc.booth_id = tie.booth_id
                     AND bc.created_at = tie.created_at
                     AND bc.congestion_id = tie.max_id
                    """,
            nativeQuery = true
    )
    List<BoothCongestion> findLatestByBoothIds(@Param("boothIds") Collection<Long> boothIds);

    @Query(
            value = """
                    SELECT COUNT(DISTINCT bc.booth_id)
                    FROM booth_congestion bc
                    INNER JOIN booth_info bi ON bi.booth_id = bc.booth_id
                    WHERE bi.festival_id = :festivalId
                    """,
            nativeQuery = true
    )
    long countDistinctBoothsWithCongestion(@Param("festivalId") Long festivalId);
}
