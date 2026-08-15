package com.example.chookjibupadmin.visitor.command.application.dto;

/**
 * 방문 인원 수 입력 요청이다.
 */
public record UpdateVisitorCountCommand(
        int visitorCount
) {
}
