package com.example.chookjibupadmin.admin.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자의 조직 내 직급을 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRank {

    private String value;

    private AdminRank(String value) {
        this.value = normalize(value);
    }

    /**
     * 직급을 검증하고 정규화해 값 객체로 변환한다.
     */
    public static AdminRank of(String value) {
        return new AdminRank(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (trimmed.length() > 50) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return trimmed;
    }
}
