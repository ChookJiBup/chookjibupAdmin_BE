package com.example.chookjibupadmin.admin.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 로그인 이메일을 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminEmail {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    // TODO(auth): 실제 운영 전 로컬 테스트용 naver.com 허용을 삭제한다.
    private static final Set<String> OFFICIAL_EXACT_DOMAINS = Set.of(
            "korea.kr",
            "naver.com"
    );
    private static final String GOVERNMENT_DOMAIN_SUFFIX = ".go.kr";

    private String value;

    private AdminEmail(String value) {
        this.value = normalize(value);
    }

    /**
     * 문자열 이메일을 검증한 뒤 값 객체로 변환한다.
     */
    public static AdminEmail of(String value) {
        return new AdminEmail(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String normalized = trimmed.toLowerCase();
        if (!isOfficialGovernmentDomain(normalized)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_DOMAIN_NOT_ALLOWED);
        }

        return normalized;
    }

    private boolean isOfficialGovernmentDomain(String value) {
        String domain = value.substring(value.indexOf('@') + 1);
        return OFFICIAL_EXACT_DOMAINS.contains(domain)
                || domain.equals("go.kr")
                || domain.endsWith(GOVERNMENT_DOMAIN_SUFFIX);
    }
}
