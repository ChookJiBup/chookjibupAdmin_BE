package com.example.chookjibupadmin.map.command.application.dto;

import java.util.List;

public record SaveRoadmapDraftCommand(
        long baseRevision,
        List<RoadmapNodeChangeCommand> nodes
) {
}
