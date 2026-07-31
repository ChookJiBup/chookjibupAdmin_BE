package com.example.chookjibupadmin.festival.query.application;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryProjection;
import com.example.chookjibupadmin.festival.query.repository.InternalFestivalQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서버용 축제 조회 저장소를 감싸는 query wrapper service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalFestivalQueryService {

    private final InternalFestivalQueryRepository queryRepository;

    /**
     * 사용자 서버에 전달할 축제 목록 projection을 조회한다.
     */
    public Page<InternalFestivalSummaryProjection> searchFestivals(
            InternalFestivalSearchCondition condition,
            Pageable pageable
    ) {
        return queryRepository.searchFestivals(condition, pageable);
    }
}
