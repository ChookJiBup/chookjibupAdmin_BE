package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeriesRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 묶음 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalSeriesService {

    private final FestivalSeriesRepository festivalSeriesRepository;

    /**
     * 축제 묶음을 저장한다.
     */
    @Transactional
    public FestivalSeries save(FestivalSeries festivalSeries) {
        return festivalSeriesRepository.save(festivalSeries);
    }

    /**
     * 내부 식별자로 축제 묶음을 조회한다.
     */
    public FestivalSeries getById(Long seriesId) {
        return festivalSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FESTIVAL_SERIES_NOT_FOUND
                ));
    }

    /**
     * 외부 UUID로 축제 묶음을 조회한다.
     */
    public FestivalSeries getByPublicId(UUID publicId) {
        return festivalSeriesRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FESTIVAL_SERIES_NOT_FOUND
                ));
    }

    /**
     * 정규화된 이름으로 축제 묶음을 조회한다.
     */
    public Optional<FestivalSeries> findByNormalizedName(String normalizedName) {
        return festivalSeriesRepository.findByNormalizedName(normalizedName);
    }
}
