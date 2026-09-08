package com.example.chookjibupadmin.operator.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현장 스태프가 담당하는 근무구역 값이다. 입력하지 않을 수 있다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldStaffDepartment {

    private static final int MAX_LENGTH = 100;

    private String value;

    private FieldStaffDepartment(String value) {
        this.value = normalize(value);
    }

    /**
     * 근무구역 문자열을 정규화해 값 객체로 변환한다. 비어 있으면 값 없음으로 둔다.
     */
    public static FieldStaffDepartment of(String value) {
        return new FieldStaffDepartment(value);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return trimmed;
    }
}
