package com.example.chookjibupadmin.map.command.application.dto;

/**
 * 삭제할 배치도의 외부 저장소 Object Key를 전달한다.
 */
public record FestivalMapDeletionTarget(
        String originalObjectKey,
        String displayObjectKey,
        String analysisObjectKey
) {
}
