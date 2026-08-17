package com.example.chookjibupadmin.admin.command.domain.vo;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
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
    private static final Set<String> OFFICIAL_EXACT_DOMAINS = Set.of("korea.kr");
    private static final String GOVERNMENT_DOMAIN_SUFFIX = ".go.kr";

    private String value;

    private AdminEmail(String value) {
        this.value = normalize(value);
    }

    /**
     * 문자열 이메일 형식만 검증한 뒤 값 객체로 변환한다.
     */
    public static AdminEmail of(String value) {
        return new AdminEmail(value);
    }

    /**
     * 계정 종류에 맞는 이메일 도메인인지 검증한다.
     */
    public static AdminEmail of(String value, AccountKind accountKind) {
        AdminEmail email = of(value);
        email.validateForAccountKind(accountKind);
        return email;
    }

    /**
     * 정부 공식 이메일 도메인인지 확인한다.
     */
    public boolean isGovernmentDomain() {
        return isOfficialGovernmentDomain(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return trimmed.toLowerCase();
    }

    private void validateForAccountKind(AccountKind accountKind) {
        if (accountKind == AccountKind.GOVERNMENT && !isGovernmentDomain()) {
            throw new CustomException(ErrorCode.AUTH_GOVERNMENT_EMAIL_REQUIRED);
        }
        if (accountKind == AccountKind.CONTRACTOR && isGovernmentDomain()) {
            throw new CustomException(ErrorCode.AUTH_CONTRACTOR_GOVERNMENT_EMAIL_NOT_ALLOWED);
        }
    }

    private static boolean isOfficialGovernmentDomain(String value) {
        String domain = value.substring(value.indexOf('@') + 1);
        return OFFICIAL_EXACT_DOMAINS.contains(domain)
                || domain.equals("go.kr")
                || domain.endsWith(GOVERNMENT_DOMAIN_SUFFIX);
    }
}
