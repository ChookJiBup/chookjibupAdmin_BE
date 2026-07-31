package com.example.chookjibupadmin.festival.query.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InternalFestivalSearchConditionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("검색어를 trim 후 소문자로 정규화한다")
        void success_Constructor_NormalizedKeyword() {
            // given
            String keyword = "  MAPO  ";

            // when
            InternalFestivalSearchCondition condition =
                    new InternalFestivalSearchCondition(
                            null,
                            keyword,
                            LocalDate.of(2026, 10, 9)
                    );

            // then
            assertThat(condition.keyword()).isEqualTo("mapo");
        }

        @Test
        @DisplayName("빈 검색어는 null로 변환한다")
        void success_Constructor_BlankKeywordBoundary() {
            // given
            String keyword = " ";

            // when
            InternalFestivalSearchCondition condition =
                    new InternalFestivalSearchCondition(
                            null,
                            keyword,
                            LocalDate.of(2026, 10, 9)
                    );

            // then
            assertThat(condition.keyword()).isNull();
        }

        @Test
        @DisplayName("기준일이 null이면 CustomException을 던진다")
        void fail_Constructor_CustomException_NullToday() {
            // given
            LocalDate today = null;

            // when & then
            assertThatThrownBy(() -> new InternalFestivalSearchCondition(
                    null,
                    null,
                    today
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
