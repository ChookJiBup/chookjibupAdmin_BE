package com.example.chookjibupadmin.map.command.application.dto;

import java.util.List;

public record SaveRoadmapDraftCommand(
        long baseRevision,
        List<RoadmapNodeChangeCommand> nodes,
        List<RoadmapZoneCommand> zones
) {

    public SaveRoadmapDraftCommand(long baseRevision, List<RoadmapNodeChangeCommand> nodes) {
        this(baseRevision, nodes, null);
    }
}
