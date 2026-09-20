package com.example.chookjibupadmin.festival.support;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 축제 현장 QR코드가 가리킬 "리뷰 작성 페이지" URL을 만든다.
 *
 * <p>사용자 프런트(chookjibupUser_FE)의 배포 주소는 https://user.chookjibup.store 로
 * 고정이라(관리자 프런트와는 다른 앱/도메인) application.yml로 빼지 않고 상수로 둔다.</p>
 *
 * <p>리뷰 작성 페이지는 {@code /festivals/{festivalPublicId}/review} 경로이고, QR코드로
 * 들어왔다는 표시로 {@code ?source=qr} 쿼리파라미터를 붙인다 — 그래야 사용자 프런트가
 * "현장(QR) 리뷰"로 인식해서 비로그인 작성을 허용한다(REVIEW01 화면, chookjibupUser_FE의
 * ReviewWritePanel 참고).</p>
 *
 * <p>QR 이미지 자체는 이 값을 받은 프런트가 직접 그린다 — 백엔드는 URL 문자열만 만든다.</p>
 */
@Component
public class ReviewQrUrlBuilder {

    private static final String USER_FRONTEND_BASE_URL = "https://user.chookjibup.store";

    public String buildReviewUrl(UUID festivalPublicId) {
        return USER_FRONTEND_BASE_URL + "/festivals/" + festivalPublicId + "/review?source=qr";
    }
}