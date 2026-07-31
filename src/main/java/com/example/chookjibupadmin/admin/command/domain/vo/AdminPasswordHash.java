package com.example.chookjibupadmin.admin.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 비밀번호 해시 값을 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminPasswordHash {

    private String value;

    private AdminPasswordHash(String value) {
        this.value = validate(value);
    }

    /**
     * 이미 해시된 비밀번호 문자열을 검증한 뒤 값 객체로 변환한다.
     */
    public static AdminPasswordHash of(String value) {
        return new AdminPasswordHash(value);
    }

    private String validate(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return value;
    }
}
