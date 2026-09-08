package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.PublishedRoadmap;

public record PublishRoadmapResponse(
        String roadmapStatus,
        long publishedVersion,
        int publishedBoothCount
) {

    public static PublishRoadmapResponse from(PublishedRoadmap published) {
        return new PublishRoadmapResponse(
                published.roadmapStatus(),
                published.publishedVersion(),
                published.publishedBoothCount()
        );
    }
}
