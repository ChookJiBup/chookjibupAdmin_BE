package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.query.application.dto.AdminNameEmailSearchTarget;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 관리자 이름과 이메일을 대상으로 대소문자를 구분하지 않는 부분 일치 검색을 수행한다.
 *
 * <p>이름 또는 이메일에 검색어가 그대로 들어 있는 계정만 결과에 포함한다.
 * 과거에는 흩어진 문자 일치(subsequence)와 편집 거리 기반 오타 보정까지 허용했으나,
 * 「연결테스트02」로 검색했을 때 「연결테스트03」이 나오는 것처럼
 * 검색어와 무관한 계정이 섞여 나와 부분 일치만 남긴다.
 */
@Component
public class AdminNameEmailSearchMatcher {

    private static final int NO_MATCH = -1;
    private static final int EXACT_SCORE = 10_000;
    private static final int PREFIX_SCORE = 8_000;
    private static final int CONTAINS_SCORE = 7_000;
    private static final int MAX_LENGTH_PENALTY = 1_000;

    /**
     * 이름 또는 이메일에 검색어가 포함된 관리자를 관련도순으로 반환한다.
     */
    public <T extends AdminNameEmailSearchTarget> List<T> search(
            List<T> candidates,
            String keyword
    ) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isEmpty()) {
            return List.copyOf(candidates);
        }

        String compactKeyword = removeWhitespace(normalizedKeyword);

        return candidates.stream()
                .map(candidate -> new ScoredCandidate<>(
                        candidate,
                        score(candidate, normalizedKeyword, compactKeyword)
                ))
                .filter(candidate -> candidate.score() != NO_MATCH)
                .sorted(Comparator.comparingInt(
                                (ScoredCandidate<T> candidate) -> candidate.score()
                        )
                        .reversed())
                .map(ScoredCandidate::candidate)
                .toList();
    }

    /**
     * 이름과 이메일 중 더 관련도가 높은 쪽의 점수를 반환한다.
     *
     * <p>이름은 「김 관 리」처럼 공백이 섞여 저장될 수 있어 공백을 지운 값끼리 비교한다.
     * 이메일에는 공백이 없으므로 검색어를 그대로 쓴다.
     */
    private int score(
            AdminNameEmailSearchTarget candidate,
            String keyword,
            String compactKeyword
    ) {
        String name = removeWhitespace(normalize(candidate.name()));
        String email = normalize(candidate.email());

        return Math.max(
                scoreValue(name, compactKeyword),
                scoreValue(email, keyword)
        );
    }

    private int scoreValue(String value, String keyword) {
        if (value.isEmpty() || keyword.isEmpty()) {
            return NO_MATCH;
        }
        if (value.equals(keyword)) {
            return EXACT_SCORE;
        }
        if (value.startsWith(keyword)) {
            return PREFIX_SCORE - lengthDifference(value, keyword);
        }

        int containsIndex = value.indexOf(keyword);
        if (containsIndex >= 0) {
            return CONTAINS_SCORE - containsIndex;
        }

        return NO_MATCH;
    }

    private int lengthDifference(String value, String keyword) {
        return Math.min(MAX_LENGTH_PENALTY, value.length() - keyword.length());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String removeWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private record ScoredCandidate<T>(T candidate, int score) {
    }
}
