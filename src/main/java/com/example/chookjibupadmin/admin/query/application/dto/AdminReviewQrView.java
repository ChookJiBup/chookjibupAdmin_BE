package com.example.chookjibupadmin.admin.query.application.dto;

import java.util.UUID;

/**
 * 관리자 "리뷰 QR 보기" 화면에 필요한 리뷰 작성 URL이다. QR 이미지는 이 URL을 받은
 * 프런트가 직접 그린다 — 백엔드는 URL만 만든다.
 */
public record AdminReviewQrView(
        UUID festivalId,
        String reviewUrl
) {
}