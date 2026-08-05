package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMapRepository;
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

    public FestivalMap getByPublicId(UUID publicId) {
        return festivalMapRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FESTIVAL_MAP_NOT_FOUND
                ));
    }

    public FestivalMap getById(Long id) {
        return festivalMapRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND));
    }

    public FestivalMap getByPublicIdForUpdate(UUID publicId) {
        return festivalMapRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FESTIVAL_MAP_NOT_FOUND
                ));
    }

    public boolean existsByLocationId(Long locationId) {
        return festivalMapRepository.existsByLocationId(locationId);
    }
}
