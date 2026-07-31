package com.example.chookjibupadmin.admin.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자가 소속된 기관 내부 부서를 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDepartment {

    private String value;

    private AdminDepartment(String value) {
        this.value = normalize(value);
    }

    /**
     * 부서명을 검증하고 정규화해 값 객체로 변환한다.
     */
    public static AdminDepartment of(String value) {
        return new AdminDepartment(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return trimmed;
    }
}
