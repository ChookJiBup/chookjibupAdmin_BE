package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.SavedRoadmapDraft;

public record SaveRoadmapDraftResponse(long editRevision) {

    public static SaveRoadmapDraftResponse from(SavedRoadmapDraft result) {
        return new SaveRoadmapDraftResponse(result.editRevision());
    }
}
