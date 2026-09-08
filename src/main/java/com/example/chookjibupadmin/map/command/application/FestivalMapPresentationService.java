package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentationRepository;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 지도 표시 설정 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalMapPresentationService {

    private final FestivalMapPresentationRepository presentationRepository;

    @Transactional
    public FestivalMapPresentation save(FestivalMapPresentation presentation) {
        return presentationRepository.save(presentation);
    }

    public Optional<FestivalMapPresentation> findByMapId(Long mapId) {
        return presentationRepository.findByMapId(mapId);
    }

    public Optional<FestivalMapPresentation> findByMapIdForUpdate(Long mapId) {
        return presentationRepository.findByMapIdForUpdate(mapId);
    }

    @Transactional
    public FestivalMapPresentation getOrCreateForUpdate(Long mapId, Long festivalId) {
        return presentationRepository.findByMapIdForUpdate(mapId)
                .orElseGet(() -> FestivalMapPresentation.createEmpty(mapId, festivalId));
    }

    @Transactional
    /**
     * 교체 전에 쓰던 이미지 키를 함께 돌려준다. 호출 쪽이 트랜잭션이 끝난 뒤 그 객체를
     * 지울 수 있게 하기 위해서다 — 예전에는 새 이미지를 올리고 나면 옛 객체가 지도
     * 삭제(purge) 때까지 스토리지에 그대로 남았다.
     */
    public record ReplacedOverlay(
            FestivalMapPresentation presentation,
            MapImageObjectKey previousImageKey
    ) {
    }

    public ReplacedOverlay replaceOverlayImage(
            Long mapId,
            Long festivalId,
            MapImageObjectKey imageKey,
            UUID assetId,
            int imageWidth,
            int imageHeight,
            MapImageAnchor fallbackAnchor
    ) {
        FestivalMapPresentation presentation = getOrCreateForUpdate(mapId, festivalId);
        MapImageObjectKey previousImageKey = presentation.getOverlayImageKey();
        boolean keepExistingAnchor = presentation.getOverlayImageAnchor() != null;
        presentation.updateOverlay(
                imageKey,
                assetId,
                imageWidth,
                imageHeight,
                keepExistingAnchor ? null : fallbackAnchor
        );
        // 업로드 직후 조회·FE 병합 전에 이미지가 보이도록 켠다.
        // opacity/clip은 기존 값을 유지하고, 초안 저장에서 FE 편집값을 반영한다.
        presentation.setOverlayVisible(true);
        return new ReplacedOverlay(save(presentation), previousImageKey);
    }

    @Transactional
    public void deleteByMapIdIn(Iterable<Long> mapIds) {
        presentationRepository.deleteByMapIdIn(mapIds);
    }
}
