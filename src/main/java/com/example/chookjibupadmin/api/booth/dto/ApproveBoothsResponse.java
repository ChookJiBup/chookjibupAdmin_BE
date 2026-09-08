package com.example.chookjibupadmin.api.booth.dto;

import com.example.chookjibupadmin.booth.command.application.dto.ApproveBoothsResult;
import java.util.List;

/** 일괄 승인 결과. 이번에 새로 승인된 부스만 담긴다. */
public record ApproveBoothsResponse(int approvedCount, List<ApproveBoothResponse> booths) {

    public static ApproveBoothsResponse from(ApproveBoothsResult result) {
        List<ApproveBoothResponse> booths = result.approved().stream()
                .map(ApproveBoothResponse::from)
                .toList();
        return new ApproveBoothsResponse(booths.size(), booths);
    }
}
