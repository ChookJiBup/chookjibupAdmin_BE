package com.example.demoadmin.festival.query.repository;

import com.example.demoadmin.festival.query.application.dto.FestivalSeriesSearchView;
import java.util.List;

/**
 * 축제 등록용 기존 축제 시리즈 조회 계약이다.
 */
public interface FestivalSeriesQueryRepository {

    /**
     * 축제명에 검색어가 포함된 시리즈와 각 시리즈의 최근 개최 정보를 조회한다.
     */
    List<FestivalSeriesSearchView> search(String keyword, int limit);
}
