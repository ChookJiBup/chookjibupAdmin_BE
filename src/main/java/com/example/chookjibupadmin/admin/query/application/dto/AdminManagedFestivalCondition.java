package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;

/**
 * 관리자 개인 관리 축제 조회 조건이다.
 */
public record AdminManagedFestivalCondition(
        AdminRole role,
        Integer year,
        String keyword,
        FestivalProgressStatus progressStatus
) {

    public AdminManagedFestivalCondition(
            AdminRole role,
            Integer year,
            String keyword
    ) {
        this(role, year, keyword, null);
    }

    /**
     * 검색어를 조회 조건용으로 정리한다.
     */
    public AdminManagedFestivalCondition normalize() {
        return new AdminManagedFestivalCondition(
                role,
                year,
                normalizeKeyword(keyword),
                progressStatus
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim().toLowerCase();
    }
}
