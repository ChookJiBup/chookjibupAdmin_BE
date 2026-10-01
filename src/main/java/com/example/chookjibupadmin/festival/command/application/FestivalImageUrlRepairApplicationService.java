package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalPublicImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리 대상 대표 이미지 주소를 비공개 S3를 중계하는 공개 API 주소로 보정한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.map.storage", name = "provider", havingValue = "s3")
public class FestivalImageUrlRepairApplicationService implements ApplicationRunner {

    private final FestivalService festivalService;
    private final FestivalPublicImageUrlBuilder publicImageUrlBuilder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int repaired = 0;
        for (Festival festival : festivalService.findAllWithManagedRepresentativeImageUrl()) {
            festival.assignRepresentativeImage(publicImageUrlBuilder.build(festival.getPublicId()));
            repaired += 1;
        }
        if (repaired > 0) {
            log.info("Repaired legacy festival representative image URLs: count={}", repaired);
        }
    }
}
