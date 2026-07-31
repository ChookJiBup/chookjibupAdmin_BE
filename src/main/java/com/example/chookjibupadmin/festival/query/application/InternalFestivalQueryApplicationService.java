package com.example.chookjibupadmin.festival.query.application;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalProgressStatus;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryView;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서버용 축제 목록 조회 흐름을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalFestivalQueryApplicationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final InternalFestivalQueryService queryService;
    private final Clock clock;

    /**
     * 진행 상태와 검색 조건에 맞는 사용자 서버용 축제 목록을 조회한다.
     */
    public Page<InternalFestivalSummaryView> searchFestivals(
            InternalFestivalProgressStatus progressStatus,
            String keyword,
            Integer page,
            Integer size
    ) {
        LocalDate today = LocalDate.now(clock);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        InternalFestivalSearchCondition condition =
                new InternalFestivalSearchCondition(
                        progressStatus,
                        keyword,
                        today
                );

        return queryService.searchFestivals(condition, pageable)
                .map(projection -> projection.toView(today));
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return size;
    }
}
