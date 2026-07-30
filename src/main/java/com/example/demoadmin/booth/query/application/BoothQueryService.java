package com.example.demoadmin.booth.query.application;

import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import com.example.demoadmin.booth.query.repository.BoothQueryRepository;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 부스 조회 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueryService {

    private final BoothQueryRepository boothQueryRepository;

    public List<BoothView> findAllByFestivalId(Long festivalId) {
        return boothQueryRepository.findAllByFestivalId(festivalId);
    }

    public BoothView getByFestivalIdAndPublicId(
            Long festivalId,
            UUID boothId
    ) {
        return boothQueryRepository.findByFestivalIdAndPublicId(festivalId, boothId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_NOT_FOUND));
    }

    public List<BoothQueueLineView> findQueueLinesByFestivalIdAndBoothPublicId(
            Long festivalId,
            UUID boothId
    ) {
        return boothQueryRepository.findQueueLinesByFestivalIdAndBoothPublicId(
                festivalId,
                boothId
        );
    }
}
