package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalImageObjectKey;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 과거 요청 호스트 기반 이미지 주소를 실제 S3 공개 객체 주소로 보정한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.map.storage", name = "provider", havingValue = "s3")
public class FestivalImageUrlRepairApplicationService implements ApplicationRunner {

    private final FestivalService festivalService;
    private final MapImageStoragePort imageStoragePort;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int repaired = 0;
        for (Festival festival : festivalService.findAllWithLegacyRepresentativeImageUrl()) {
            festival.assignRepresentativeImage(imageStoragePort.createPublicUrl(
                    FestivalImageObjectKey.representative(festival.getPublicId())
            ).toString());
            repaired += 1;
        }
        if (repaired > 0) {
            log.info("Repaired legacy festival representative image URLs: count={}", repaired);
        }
    }
}
