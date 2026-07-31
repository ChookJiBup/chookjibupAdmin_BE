package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.query.application.dto.AdminNameEmailSearchTarget;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 관리자 이름과 이메일을 대상으로 부분 일치와 제한적인 오타 검색을 수행한다.
 */
@Component
public class AdminNameEmailSearchMatcher {

    private static final int NO_MATCH = -1;
    private static final int EXACT_SCORE = 10_000;
    private static final int PREFIX_SCORE = 8_000;
    private static final int CONTAINS_SCORE = 7_000;
    private static final int SUBSEQUENCE_SCORE = 5_000;
    private static final int TYPO_SCORE = 3_000;
    private static final int MIN_TYPO_QUERY_LENGTH = 3;
    private static final double MIN_TYPO_SIMILARITY = 0.6;

    /**
     * 검색어와 일치하는 관리자를 관련도순으로 반환한다.
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

    private int score(
            AdminNameEmailSearchTarget candidate,
            String keyword,
            String compactKeyword
    ) {
        String name = removeWhitespace(normalize(candidate.name()));
        String email = normalize(candidate.email());
        String emailLocalPart = extractLocalPart(email);

        return Math.max(
                scoreValue(name, compactKeyword),
                Math.max(
                        scoreValue(email, keyword),
                        scoreValue(emailLocalPart, keyword)
                )
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

        int subsequenceGap = subsequenceGap(value, keyword);
        if (subsequenceGap >= 0) {
            return SUBSEQUENCE_SCORE - subsequenceGap;
        }

        return typoScore(value, keyword);
    }

    private int typoScore(String value, String keyword) {
        if (keyword.length() < MIN_TYPO_QUERY_LENGTH) {
            return NO_MATCH;
        }

        double similarity = bestPartialSimilarity(value, keyword);
        if (similarity < MIN_TYPO_SIMILARITY) {
            return NO_MATCH;
        }

        return TYPO_SCORE + (int) Math.round(similarity * 100);
    }

    private double bestPartialSimilarity(String value, String keyword) {
        int minimumWindowLength = Math.max(1, keyword.length() - 1);
        int maximumWindowLength = Math.min(value.length(), keyword.length() + 1);
        double bestSimilarity = 0.0;

        for (int windowLength = minimumWindowLength;
                windowLength <= maximumWindowLength;
                windowLength++) {
            for (int start = 0; start + windowLength <= value.length(); start++) {
                String window = value.substring(start, start + windowLength);
                int distance = damerauLevenshteinDistance(keyword, window);
                int maximumLength = Math.max(keyword.length(), window.length());
                double similarity = 1.0 - ((double) distance / maximumLength);
                bestSimilarity = Math.max(bestSimilarity, similarity);
            }
        }

        return bestSimilarity;
    }

    private int damerauLevenshteinDistance(String left, String right) {
        int[][] distances = new int[left.length() + 1][right.length() + 1];

        for (int leftIndex = 0; leftIndex <= left.length(); leftIndex++) {
            distances[leftIndex][0] = leftIndex;
        }
        for (int rightIndex = 0; rightIndex <= right.length(); rightIndex++) {
            distances[0][rightIndex] = rightIndex;
        }

        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int substitutionCost = left.charAt(leftIndex - 1)
                        == right.charAt(rightIndex - 1) ? 0 : 1;
                distances[leftIndex][rightIndex] = Math.min(
                        Math.min(
                                distances[leftIndex - 1][rightIndex] + 1,
                                distances[leftIndex][rightIndex - 1] + 1
                        ),
                        distances[leftIndex - 1][rightIndex - 1] + substitutionCost
                );

                if (leftIndex > 1
                        && rightIndex > 1
                        && left.charAt(leftIndex - 1) == right.charAt(rightIndex - 2)
                        && left.charAt(leftIndex - 2) == right.charAt(rightIndex - 1)) {
                    distances[leftIndex][rightIndex] = Math.min(
                            distances[leftIndex][rightIndex],
                            distances[leftIndex - 2][rightIndex - 2] + substitutionCost
                    );
                }
            }
        }

        return distances[left.length()][right.length()];
    }

    private int subsequenceGap(String value, String keyword) {
        int keywordIndex = 0;
        int firstMatchIndex = -1;
        int lastMatchIndex = -1;

        for (int valueIndex = 0;
                valueIndex < value.length() && keywordIndex < keyword.length();
                valueIndex++) {
            if (value.charAt(valueIndex) == keyword.charAt(keywordIndex)) {
                if (firstMatchIndex < 0) {
                    firstMatchIndex = valueIndex;
                }
                lastMatchIndex = valueIndex;
                keywordIndex++;
            }
        }

        if (keywordIndex != keyword.length()) {
            return NO_MATCH;
        }

        return lastMatchIndex - firstMatchIndex - keyword.length() + 1;
    }

    private int lengthDifference(String value, String keyword) {
        return Math.min(1_000, value.length() - keyword.length());
    }

    private String extractLocalPart(String email) {
        int atIndex = email.indexOf('@');
        return atIndex < 0 ? email : email.substring(0, atIndex);
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
