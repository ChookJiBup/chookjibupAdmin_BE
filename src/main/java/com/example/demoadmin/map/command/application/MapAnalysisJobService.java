package com.example.demoadmin.map.command.application;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.MapAnalysisJob;
import com.example.demoadmin.map.command.domain.MapAnalysisJobRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치도 분석 작업 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapAnalysisJobService {

    private final MapAnalysisJobRepository mapAnalysisJobRepository;

    @Transactional
    public MapAnalysisJob save(MapAnalysisJob analysisJob) {
        return mapAnalysisJobRepository.save(analysisJob);
    }

    public MapAnalysisJob getById(Long analysisJobId) {
        return mapAnalysisJobRepository.findById(analysisJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAP_ANALYSIS_JOB_NOT_FOUND));
    }

    public MapAnalysisJob getByPublicId(UUID publicId) {
        return mapAnalysisJobRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAP_ANALYSIS_JOB_NOT_FOUND));
    }
}
