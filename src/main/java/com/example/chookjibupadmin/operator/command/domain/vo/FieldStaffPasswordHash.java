package com.example.chookjibupadmin.operator.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현장 스태프 비밀번호 해시 값이다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldStaffPasswordHash {

    private static final int MAX_LENGTH = 255;

    private String value;

    private FieldStaffPasswordHash(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = value;
    }

    /**
     * 인코딩된 비밀번호 해시를 생성한다.
     */
    public static FieldStaffPasswordHash of(String value) {
        return new FieldStaffPasswordHash(value);
    }
}
