package com.example.demoadmin.map.command.application;

import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import com.example.demoadmin.map.command.application.dto.MapAnalysisResultView;
import com.example.demoadmin.map.command.application.dto.PreparedMapAnalysis;
import com.example.demoadmin.map.command.application.port.MapAnalysisResult;
import com.example.demoadmin.map.command.application.port.MapImageAnalyzer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 배치도 이미지 분석 생성 흐름을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class MapAnalysisApplicationService {

    private final MapAnalysisPersistenceService mapAnalysisPersistenceService;
    private final MapImageAnalyzer mapImageAnalyzer;

    /**
     * 테스트 리소스 배치도를 분석하고 검수 대기 지도 객체로 저장한다.
     */
    public MapAnalysisResultView analyzeTestMap(
            UUID festivalId,
            CreateTestMapAnalysisCommand command,
            AdminPrincipal principal
    ) {
        PreparedMapAnalysis prepared = mapAnalysisPersistenceService.prepare(
                festivalId,
                command,
                principal
        );

        try {
            MapAnalysisResult result = mapImageAnalyzer.analyze(
                    prepared.analysisRequest()
            );
            return mapAnalysisPersistenceService.complete(prepared, result);
        } catch (CustomException exception) {
            mapAnalysisPersistenceService.fail(
                    prepared.analysisJobId(),
                    exception.getMessage()
            );
            throw exception;
        } catch (RuntimeException exception) {
            mapAnalysisPersistenceService.fail(
                    prepared.analysisJobId(),
                    exception.getMessage()
            );
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
