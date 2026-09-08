package com.example.chookjibupadmin.booth.command.application.dto;

import java.util.List;

/** 한 번에 승인한 부스들. 이미 승인돼 있던 노드는 담기지 않는다. */
public record ApproveBoothsResult(List<ApproveBoothResult> approved) {
}
