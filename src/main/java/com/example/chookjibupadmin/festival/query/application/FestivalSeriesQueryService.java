package com.example.chookjibupadmin.festival.query.application;

import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.chookjibupadmin.festival.query.repository.FestivalSeriesQueryRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 시리즈 Query Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalSeriesQueryService {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_LIMIT = 20;

    private final FestivalSeriesQueryRepository queryRepository;

    /**
     * 검색 조건을 검증한 뒤 기존 축제 시리즈를 조회한다.
     */
    public List<FestivalSeriesSearchView> search(
            String keyword,
            int limit
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        validateLimit(limit);
        return queryRepository.search(normalizedKeyword, limit);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String normalized = keyword.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
