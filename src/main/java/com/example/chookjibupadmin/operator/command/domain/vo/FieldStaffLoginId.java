package com.example.chookjibupadmin.operator.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제별 현장 스태프 로그인 아이디 값이다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldStaffLoginId {

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 30;
    private static final String LOGIN_ID_PATTERN = "^[a-z0-9._-]+$";

    private String value;

    private FieldStaffLoginId(String value) {
        String normalized = normalize(value);
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (!normalized.matches(LOGIN_ID_PATTERN)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = normalized;
    }

    /**
     * 로그인 아이디를 소문자 기준으로 정규화해 생성한다.
     */
    public static FieldStaffLoginId of(String value) {
        return new FieldStaffLoginId(value);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
