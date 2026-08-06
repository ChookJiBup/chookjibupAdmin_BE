package com.example.chookjibupadmin.map.analysis.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MapAnalysisJobJpaRepositoryTest {

    @Autowired
    private MapAnalysisJobJpaRepository repository;

    @Test
    @DisplayName("재시도 시각이 지난 가장 오래된 대기 작업부터 조회한다")
    void success_FindPendingForUpdate_OldestRunnableJob() {
        // given
        MapAnalysisJob first = repository.save(job(1L));
        repository.save(job(2L));
        MapAnalysisJob delayed = job(3L);
        delayed.start();
        delayed.retry("OPENAI_HTTP_429", "retry later");
        repository.save(delayed);
        repository.flush();

        // when
        List<MapAnalysisJob> result = repository.findPendingForUpdate(
                MapAnalysisJobStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, 1)
        );

        // then
        assertThat(result).containsExactly(first);
    }

    private MapAnalysisJob job(Long mapId) {
        return MapAnalysisJob.pending(
                mapId,
                "openai",
                "gpt-5.6",
                "analysis-key-" + mapId,
                "a".repeat(64),
                1200,
                800
        );
    }
}
