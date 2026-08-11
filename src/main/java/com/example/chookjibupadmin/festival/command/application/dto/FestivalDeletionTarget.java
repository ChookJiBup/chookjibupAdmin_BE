package com.example.chookjibupadmin.festival.command.application.dto;

import java.util.List;

/**
 * 축제 삭제 전에 외부 저장소에서 제거해야 하는 객체 키 목록이다.
 */
public record FestivalDeletionTarget(List<String> objectKeys) {

    public FestivalDeletionTarget {
        objectKeys = List.copyOf(objectKeys);
    }
}
