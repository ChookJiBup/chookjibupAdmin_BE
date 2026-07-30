package com.example.demoadmin.map.command.application;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.FestivalMap;
import com.example.demoadmin.map.command.domain.FestivalMapRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 배치도 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalMapService {

    private final FestivalMapRepository festivalMapRepository;

    @Transactional
    public FestivalMap save(FestivalMap festivalMap) {
        return festivalMapRepository.save(festivalMap);
    }

    public FestivalMap getById(Long festivalMapId) {
        return festivalMapRepository.findById(festivalMapId)
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND));
    }

    public FestivalMap getByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    ) {
        return festivalMapRepository.findByFestivalIdAndPublicId(festivalId, publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND));
    }
}
