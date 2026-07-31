package com.example.chookjibupadmin.operator.query.application;

import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;

/**
 * 현장 스태프 목록에서 입력 중인 한글 검색어를 매칭한다.
 */
class FieldStaffSearchMatcher {

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_END = 0xD7A3;
    private static final int JUNG_COUNT = 21;
    private static final int JONG_COUNT = 28;
    private static final int SYLLABLE_BLOCK = JUNG_COUNT * JONG_COUNT;
    private static final char[] CHO_SUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ',
            'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ',
            'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char[] JUNG_SUNG = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ',
            'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ',
            'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };
    private static final char[] JONG_SUNG = {
            '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ',
            'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ',
            'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ',
            'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    boolean matches(FieldStaffView view, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword == null) {
            return true;
        }

        return contains(view.loginId(), normalizedKeyword)
                || contains(view.name(), normalizedKeyword)
                || contains(view.phoneNumber(), normalizedKeyword)
                || hangulContains(view.name(), normalizedKeyword);
    }

    private boolean contains(String value, String normalizedKeyword) {
        String normalizedValue = normalize(value);
        return normalizedValue != null
                && normalizedValue.contains(normalizedKeyword);
    }

    private boolean hangulContains(String value, String normalizedKeyword) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return false;
        }

        String decomposedValue = decomposeHangul(normalizedValue);
        String decomposedKeyword = decomposeHangul(normalizedKeyword);
        String initialValue = extractInitialConsonants(normalizedValue);

        return decomposedValue.contains(decomposedKeyword)
                || initialValue.contains(normalizedKeyword);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .trim();
    }

    private String decomposeHangul(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < HANGUL_BASE || character > HANGUL_END) {
                builder.append(character);
                continue;
            }

            int offset = character - HANGUL_BASE;
            int choIndex = offset / SYLLABLE_BLOCK;
            int jungIndex = (offset % SYLLABLE_BLOCK) / JONG_COUNT;
            int jongIndex = offset % JONG_COUNT;

            builder.append(CHO_SUNG[choIndex]);
            builder.append(JUNG_SUNG[jungIndex]);
            if (JONG_SUNG[jongIndex] != '\0') {
                builder.append(JONG_SUNG[jongIndex]);
            }
        }

        return builder.toString();
    }

    private String extractInitialConsonants(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= HANGUL_BASE && character <= HANGUL_END) {
                int offset = character - HANGUL_BASE;
                builder.append(CHO_SUNG[offset / SYLLABLE_BLOCK]);
            }
        }

        return builder.toString();
    }
}
