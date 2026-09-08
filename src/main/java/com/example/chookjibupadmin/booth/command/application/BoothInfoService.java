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

    /** 지도 노드에 딸린 운영 부스들. 노드를 지우거나 이름을 바꿀 때 함께 손보려고 쓴다. */
    public List<BoothInfo> findAllByRoadmapNodeIdIn(List<Long> roadmapNodeIds) {
        return boothInfoRepository.findAllByRoadmapNodeIdIn(roadmapNodeIds);
    }

    public void deleteAll(List<BoothInfo> booths) {
        boothInfoRepository.deleteAll(booths);
    }

    public long countByFestivalId(Long festivalId) {
        return boothInfoRepository.countByFestivalId(festivalId);
    }
}
