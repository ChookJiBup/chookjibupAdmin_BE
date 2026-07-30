package com.example.demoadmin.booth.command.application;

import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.FestivalBoothRepository;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 부스 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalBoothService {

    private final FestivalBoothRepository festivalBoothRepository;

    @Transactional
    public FestivalBooth save(FestivalBooth booth) {
        return festivalBoothRepository.save(booth);
    }

    public FestivalBooth getByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    ) {
        return festivalBoothRepository.findByFestivalIdAndPublicId(festivalId, publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_NOT_FOUND));
    }

    @Transactional
    public FestivalBooth getByFestivalIdAndPublicIdForUpdate(
            Long festivalId,
            UUID publicId
    ) {
        return festivalBoothRepository
                .findByFestivalIdAndPublicIdForUpdate(festivalId, publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_NOT_FOUND));
    }
}
