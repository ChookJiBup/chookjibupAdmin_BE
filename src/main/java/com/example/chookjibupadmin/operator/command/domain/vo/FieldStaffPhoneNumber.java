package com.example.chookjibupadmin.operator.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현장 스태프 연락처 값이다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldStaffPhoneNumber {

    private static final String PHONE_PATTERN = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";

    private String value;

    private FieldStaffPhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String normalized = value.trim().replace(" ", "");
        if (!normalized.matches(PHONE_PATTERN)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = normalized;
    }

    /**
     * 국내 휴대전화 형식의 연락처를 생성한다.
     */
    public static FieldStaffPhoneNumber of(String value) {
        return new FieldStaffPhoneNumber(value);
    }
}
