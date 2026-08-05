package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisQueueApplicationService;
import com.example.chookjibupadmin.map.command.domain.FestivalMapStorageStatus;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalMapLifecycleApplicationServiceTest {

    @InjectMocks
    private FestivalMapLifecycleApplicationService service;

    @Mock
    private FestivalMapService festivalMapService;
    @Mock
    private FestivalService festivalService;
    @Mock
    private MapAnalysisQueueApplicationService mapAnalysisQueueService;

    @Test
    @DisplayName("축제 행을 잠근 뒤 현재 배치도를 새 배치도로 교체한다")
    void success_Replace_WithFestivalLock() {
        UUID currentMapId = UUID.randomUUID();
        FestivalMap current = festivalMap(currentMapId, "current-original", "current-display");
        FestivalMap replacement = festivalMap(
                UUID.randomUUID(), "new-original", "new-display"
        );
        ReflectionTestUtils.setField(current, "id", 10L);
        current.assignLocation(30L);
        given(festivalMapService.getByPublicIdForUpdate(currentMapId))
                .willReturn(current);
        given(festivalMapService.save(replacement)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(replacement, "id", 11L);
            return replacement;
        });

        FestivalMap result = service.replace(currentMapId, 20L, replacement);

        assertThat(result).isSameAs(replacement);
        assertThat(current.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.REPLACED);
        assertThat(replacement.getReplacesMapId()).isEqualTo(10L);
        assertThat(replacement.getLocationId()).isEqualTo(30L);
        InOrder order = inOrder(festivalService, festivalMapService);
        order.verify(festivalService).getByIdForUpdate(20L);
        order.verify(festivalMapService).getByPublicIdForUpdate(currentMapId);
        org.mockito.Mockito.verify(mapAnalysisQueueService)
                .enqueueReplacement(current, replacement);
    }

    private FestivalMap festivalMap(
            UUID publicId,
            String originalKey,
            String displayKey
    ) {
        return FestivalMap.uploaded(
                publicId, 20L, FestivalMapName.of("축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of(originalKey),
                MapImageObjectKey.of(displayKey),
                MapImageObjectKey.of(displayKey + "-analysis"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100L), MapImageFileSize.of(90L),
                MapImageFileSize.of(80L),
                MapImageDimensions.of(1200, 800),
                MapImageDimensions.of(1200, 800),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)), 1L
        );
    }
}
