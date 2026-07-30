package com.example.demoadmin.address.query.application;

import com.example.demoadmin.address.query.application.dto.RoadAddressSearchResult;
import com.example.demoadmin.address.query.application.port.RoadAddressSearchPort;
import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자용 도로명주소 검색 흐름과 외부 API 입력 검증을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class RoadAddressSearchService {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_SIZE = 20;
    private static final Pattern BLOCKED_SPECIAL_CHARACTERS =
            Pattern.compile("[%=><]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> BLOCKED_SQL_KEYWORDS = Set.of(
            "OR", "SELECT", "INSERT", "DELETE", "UPDATE", "CREATE", "DROP",
            "EXEC", "UNION", "FETCH", "DECLARE", "TRUNCATE"
    );

    private final AdminAccountService adminAccountService;
    private final RoadAddressSearchPort roadAddressSearchPort;

    /**
     * 인증 관리자가 입력한 검색 조건을 검증하고 도로명주소를 조회한다.
     */
    public RoadAddressSearchResult search(
            String keyword,
            Integer page,
            Integer size,
            AdminPrincipal principal
    ) {
        validateAuthenticatedAdmin(principal);
        String normalizedKeyword = normalizeKeyword(keyword);
        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 10 : size;
        validatePage(normalizedPage);
        validateSize(normalizedSize);

        return roadAddressSearchPort.search(
                normalizedKeyword,
                normalizedPage,
                normalizedSize
        );
    }

    private void validateAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String normalized = WHITESPACE.matcher(keyword.trim()).replaceAll(" ");
        if (normalized.length() < MIN_KEYWORD_LENGTH
                || normalized.length() > MAX_KEYWORD_LENGTH
                || BLOCKED_SPECIAL_CHARACTERS.matcher(normalized).find()
                || containsBlockedSqlKeyword(normalized)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private boolean containsBlockedSqlKeyword(String keyword) {
        return Pattern.compile("[^\\p{L}\\p{N}]+")
                .splitAsStream(keyword.toUpperCase(Locale.ROOT))
                .anyMatch(BLOCKED_SQL_KEYWORDS::contains);
    }

    private void validatePage(int page) {
        if (page < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
