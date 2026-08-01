package com.example.chookjibupadmin.admin.query.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminManagedFestivalConditionTest {

    @Nested
    @DisplayName("normalize")
    class Normalize {

        @Test
        @DisplayName("검색어를 소문자와 trim 값으로 정리한다")
        void success_Normalize() {
            // given
            AdminManagedFestivalCondition condition =
                    new AdminManagedFestivalCondition(
                            AdminRole.FESTIVAL_OWNER,
                            2026,
                            " MAPO ",
                            FestivalProgressStatus.ONGOING
                    );

            // when
            AdminManagedFestivalCondition result = condition.normalize();

            // then
            assertThat(result.role()).isEqualTo(AdminRole.FESTIVAL_OWNER);
            assertThat(result.year()).isEqualTo(2026);
            assertThat(result.keyword()).isEqualTo("mapo");
            assertThat(result.progressStatus())
                    .isEqualTo(FestivalProgressStatus.ONGOING);
        }

        @Test
        @DisplayName("빈 검색어는 null로 정리한다")
        void success_Normalize_BlankKeywordBoundary() {
            // given
            AdminManagedFestivalCondition condition =
                    new AdminManagedFestivalCondition(null, null, " ");

            // when
            AdminManagedFestivalCondition result = condition.normalize();

            // then
            assertThat(result.keyword()).isNull();
        }
    }
}
