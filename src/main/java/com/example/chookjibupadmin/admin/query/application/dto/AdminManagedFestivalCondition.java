package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;

/**
 * 관리자 개인 관리 축제 조회 조건이다.
 */
public record AdminManagedFestivalCondition(
        AdminRole role,
        Integer year,
        String keyword
) {

    /**
     * 검색어를 조회 조건용으로 정리한다.
     */
    public AdminManagedFestivalCondition normalize() {
        return new AdminManagedFestivalCondition(
                role,
                year,
                normalizeKeyword(keyword)
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim().toLowerCase();
    }
}
