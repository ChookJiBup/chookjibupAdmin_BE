package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 기본 정보 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalService {

    private final FestivalRepository festivalRepository;

    /**
     * 축제 기본 정보를 저장한다.
     */
    @Transactional
    public Festival save(Festival festival) {
        return festivalRepository.save(festival);
    }

    /**
     * 내부 식별자로 축제를 조회한다.
     */
    public Festival getById(Long festivalId) {
        return festivalRepository.findById(festivalId)
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));
    }

    /**
     * 외부 UUID로 축제를 조회한다.
     */
    public Festival getByPublicId(UUID publicId) {
        return festivalRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));
    }

    /**
     * 지정한 축제 묶음의 개최 연도 중복 여부를 확인한다.
     */
    public boolean existsBySeriesIdAndYear(
            Long seriesId,
            int year
    ) {
        return festivalRepository.existsBySeriesIdAndYear(seriesId, year);
    }
}
