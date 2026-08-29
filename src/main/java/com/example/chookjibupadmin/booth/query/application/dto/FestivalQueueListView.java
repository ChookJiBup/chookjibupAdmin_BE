package com.example.chookjibupadmin.booth.query.application.dto;

import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import java.util.List;

public record FestivalQueueListView(
        Long festivalId,
        List<BoothQueueResult> queues
) {
}
