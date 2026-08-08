package com.example.chookjibupadmin.festival.command.domain;

/**
 * 축제 기본 정보의 운영 상태를 표현한다.
 */
public enum FestivalStatus {
    DRAFT;

    /**
     * 축제 기본 정보 수정이 가능한 게시 상태인지 확인한다.
     */
    public boolean canModifyBasicInfo() {
        return this == DRAFT;
    }
}
