package com.example.chookjibupadmin.map.command.application.dto;

/**
 * 부스맵 공개 결과.
 *
 * @param roadmapStatus       공개 후 로드맵 상태(항상 PUBLISHED)
 * @param publishedVersion    방문객에게 나간 편집 리비전
 * @param publishedBoothCount 방문객 지도에 실제로 보이는 부스 수
 */
public record PublishedRoadmap(
        String roadmapStatus,
        long publishedVersion,
        int publishedBoothCount
) {
}
