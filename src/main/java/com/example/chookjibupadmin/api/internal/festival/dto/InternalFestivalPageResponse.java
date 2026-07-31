package com.example.chookjibupadmin.api.internal.festival.dto;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryView;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 사용자 서버용 축제 목록 페이지 응답이다.
 */
public record InternalFestivalPageResponse(
        List<InternalFestivalSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static InternalFestivalPageResponse from(
            Page<InternalFestivalSummaryView> page
    ) {
        return new InternalFestivalPageResponse(
                page.getContent()
                        .stream()
                        .map(InternalFestivalSummaryResponse::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
