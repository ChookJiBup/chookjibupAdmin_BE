package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothInfoRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 승인 부스 Aggregate 저장소 래퍼이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothInfoService {

    private final BoothInfoRepository boothInfoRepository;

    @Transactional
    public BoothInfo save(BoothInfo boothInfo) {
        return boothInfoRepository.save(boothInfo);
    }

    public BoothInfo getById(Long boothId) {
        return boothInfoRepository.findById(boothId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_NOT_FOUND));
    }

    public Optional<BoothInfo> findByFestivalIdAndRoadmapNodeId(
            Long festivalId,
            Long roadmapNodeId
    ) {
        return boothInfoRepository.findByFestivalIdAndRoadmapNodeId(
                festivalId,
                roadmapNodeId
        );
    }

    public List<BoothInfo> findAllByFestivalId(Long festivalId) {
        return boothInfoRepository.findAllByFestivalId(festivalId);
    }

    public long countByFestivalId(Long festivalId) {
        return boothInfoRepository.countByFestivalId(festivalId);
    }
}
