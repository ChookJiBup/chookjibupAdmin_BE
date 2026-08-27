package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class BoothCongestionJpaRepositoryTest {

    @Autowired
    private BoothCongestionJpaRepository boothCongestionJpaRepository;

    @Autowired
    private BoothInfoJpaRepository boothInfoJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("동일 created_at이면 congestion_id가 큰 행을 최신으로 고른다")
    void success_FindLatest_TieBreakByCongestionId() {
        BoothInfo booth = boothInfoJpaRepository.saveAndFlush(
                BoothInfo.create(10L, 77L, "김밥부스")
        );
        LocalDateTime sameTime = LocalDateTime.of(2026, 10, 18, 12, 0);
        BoothCongestion older = BoothCongestion.recordByAdmin(
                booth.getId(),
                1L,
                5,
                BoothCongestionLevel.LOW
        );
        BoothCongestion newer = BoothCongestion.recordByAdmin(
                booth.getId(),
                1L,
                20,
                BoothCongestionLevel.HIGH
        );
        ReflectionTestUtils.setField(older, "createdAt", sameTime);
        ReflectionTestUtils.setField(older, "updatedAt", sameTime);
        ReflectionTestUtils.setField(newer, "createdAt", sameTime);
        ReflectionTestUtils.setField(newer, "updatedAt", sameTime);
        boothCongestionJpaRepository.saveAndFlush(older);
        BoothCongestion savedNewer = boothCongestionJpaRepository.saveAndFlush(newer);
        entityManager.clear();

        BoothCongestion latest = boothCongestionJpaRepository
                .findLatestByBoothId(booth.getId())
                .orElseThrow();
        List<BoothCongestion> latestRows = boothCongestionJpaRepository
                .findLatestByBoothIds(List.of(booth.getId()));

        assertThat(latest.getId()).isEqualTo(savedNewer.getId());
        assertThat(latest.getWaitMinutes()).isEqualTo(20);
        assertThat(latestRows).hasSize(1);
        assertThat(latestRows.get(0).getId()).isEqualTo(savedNewer.getId());
    }
}
