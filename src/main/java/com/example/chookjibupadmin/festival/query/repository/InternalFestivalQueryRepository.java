package com.example.chookjibupadmin.festival.query.repository;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 사용자 서버용 축제 목록 조회 저장소 계약이다.
 */
public interface InternalFestivalQueryRepository {

    /**
     * 진행 상태와 검색 조건에 맞는 축제 목록을 projection으로 조회한다.
     */
    Page<InternalFestivalSummaryProjection> searchFestivals(
            InternalFestivalSearchCondition condition,
            Pageable pageable
    );
}
