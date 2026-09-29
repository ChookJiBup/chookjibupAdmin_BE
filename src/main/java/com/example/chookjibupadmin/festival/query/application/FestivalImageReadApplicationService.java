package com.example.chookjibupadmin.festival.query.application;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.support.FestivalImageObjectKey;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 축제 대표 이미지에 접근할 수 있는 단기 서명 URL을 발급한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalImageReadApplicationService {

    private final FestivalService festivalService;
    private final MapImageStoragePort imageStoragePort;

    public MapImageReadUrl createReadUrl(UUID festivalId) {
        festivalService.getByPublicId(festivalId);
        return imageStoragePort.createReadUrl(FestivalImageObjectKey.representative(festivalId));
    }
}
