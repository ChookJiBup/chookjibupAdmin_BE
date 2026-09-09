package com.example.chookjibupadmin.operator.command.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 현장 스태프 계정 유효기간을 벗어난 이유를 날짜와 함께 안내한다.
 *
 * <p>유효기간은 계정 생성 시점에 「축제 시작일 7일 전 0시 ~ 축제 종료일 23:59」로
 * 고정 저장한다. 시작 전인지 이미 끝났는지 구분하지 않으면
 * 아직 시작하지 않은 계정도 만료로 오해하게 되므로 두 경우를 나눠서 알려 준다.</p>
 */
public final class FieldStaffValidPeriodMessage {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN);

    private FieldStaffValidPeriodMessage() {
    }

    /**
     * 유효기간 시작 전이면 로그인 가능 시작일을, 이미 지났으면 마지막 가능일을 안내한다.
     */
    public static String of(
            FieldStaffAccount account,
            LocalDateTime now
    ) {
        if (now.isBefore(account.getValidFrom())) {
            return DATE_FORMAT.format(account.getValidFrom()) + "부터 로그인할 수 있습니다.";
        }

        return DATE_FORMAT.format(account.getValidUntil()) + "까지만 로그인할 수 있었습니다.";
    }
}
