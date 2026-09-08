package com.example.chookjibupadmin.operator.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffStatus;
import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffSearchMatcherTest {

    private final FieldStaffSearchMatcher matcher = new FieldStaffSearchMatcher();

    @Nested
    @DisplayName("matches")
    class Matches {

        @Test
        @DisplayName("검색어가 없으면 매칭한다")
        void success_Matches_BlankKeywordBoundary() {
            // given
            FieldStaffView view = fieldStaffView("staff01", "이해준");

            // when
            boolean result = matcher.matches(view, " ");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("한글 초성과 조합 중간 상태로 이름을 매칭한다")
        void success_Matches_HangulComposingKeyword() {
            // given
            FieldStaffView view = fieldStaffView("staff01", "이해준");

            // when & then
            assertThat(matcher.matches(view, "ㅇ")).isTrue();
            assertThat(matcher.matches(view, "이")).isTrue();
            assertThat(matcher.matches(view, "잏")).isTrue();
            assertThat(matcher.matches(view, "이해")).isTrue();
        }

        @Test
        @DisplayName("로그인 ID와 전화번호를 매칭한다")
        void success_Matches_LoginIdAndPhoneNumber() {
            // given
            FieldStaffView view = fieldStaffView("staff01", "이해준");

            // when & then
            assertThat(matcher.matches(view, "STAFF")).isTrue();
            assertThat(matcher.matches(view, "1234")).isTrue();
        }

        @Test
        @DisplayName("일치하는 값이 없으면 매칭하지 않는다")
        void success_Matches_NotMatchedBoundary() {
            // given
            FieldStaffView view = fieldStaffView("staff01", "이해준");

            // when
            boolean result = matcher.matches(view, "김");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("근무구역으로도 매칭한다")
        void success_Matches_Department() {
            // given
            FieldStaffView view = fieldStaffView("staff01", "이해준", "정문 게이트");

            // when & then
            assertThat(matcher.matches(view, "정문")).isTrue();
            assertThat(matcher.matches(view, "ㅈㅁ")).isTrue();
        }

        @Test
        @DisplayName("근무구역이 없어도 매칭 판단은 실패하지 않는다")
        void success_Matches_NullDepartmentBoundary() {
            // given
            FieldStaffView view = fieldStaffView("staff01", "이해준", null);

            // when & then
            assertThat(matcher.matches(view, "정문")).isFalse();
        }
    }

    private FieldStaffView fieldStaffView(
            String loginId,
            String name,
            String department
    ) {
        return new FieldStaffView(
                UUID.randomUUID(),
                loginId,
                name,
                department,
                "010-1234-5678",
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59),
                FieldStaffStatus.ACTIVE
        );
    }

    private FieldStaffView fieldStaffView(String loginId, String name) {
        return new FieldStaffView(
                UUID.randomUUID(),
                loginId,
                name,
                "010-1234-5678",
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59),
                FieldStaffStatus.ACTIVE
        );
    }
}
