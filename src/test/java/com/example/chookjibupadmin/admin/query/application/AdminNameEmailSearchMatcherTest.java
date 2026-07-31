package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.query.application.dto.AdminNameEmailSearchTarget;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminNameEmailSearchMatcherTest {

    private final AdminNameEmailSearchMatcher matcher =
            new AdminNameEmailSearchMatcher();

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("이름의 공백과 검색어 대소문자를 정규화해서 검색한다")
        void success_Search_NormalizedName() {
            // given
            SearchTarget target = target("김 관 리", "manager@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), " 김관리 ");

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("이메일의 연속 부분 문자열로 검색한다")
        void success_Search_EmailContains() {
            // given
            SearchTarget target = target("이학준", "dlgkrwns213@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "Korea.kr");

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("이메일 전체가 정확히 일치하면 검색한다")
        void success_Search_ExactEmail() {
            // given
            SearchTarget target = target("이학준", "dlgkrwns213@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(
                    List.of(target),
                    " DLGKRWNS213@KOREA.KR "
            );

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("문자가 떨어져 있어도 같은 순서로 나타나면 검색한다")
        void success_Search_EmailSubsequence() {
            // given
            SearchTarget target = target("이학준", "dlgkrwns213@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "dkr");

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("이메일 로컬 부분의 제한적인 오타를 허용한다")
        void success_Search_EmailTypo() {
            // given
            SearchTarget target = target("이학준", "dlgkrwns213@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "dkkkr");

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("인접한 두 문자의 입력 순서가 바뀐 이메일 오타를 허용한다")
        void success_Search_EmailTransposition() {
            // given
            SearchTarget target = target("이학준", "dlgkr@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "dlkgr");

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("두 글자 검색어에는 오타 검색을 적용하지 않는다")
        void success_Search_TwoCharacterBoundary() {
            // given
            SearchTarget target = target("김관리", "ab@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "ac");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("세 글자 검색어부터 한 글자 오타를 허용한다")
        void success_Search_ThreeCharacterBoundary() {
            // given
            SearchTarget target = target("김관리", "adc@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "abc");

            // then
            assertThat(result).containsExactly(target);
        }

        @Test
        @DisplayName("빈 검색어이면 입력 순서를 유지해서 전체 후보를 반환한다")
        void success_Search_BlankKeywordBoundary() {
            // given
            SearchTarget first = target("김관리", "first@mapo.go.kr");
            SearchTarget second = target("이관리", "second@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(
                    List.of(first, second),
                    " "
            );

            // then
            assertThat(result).containsExactly(first, second);
        }

        @Test
        @DisplayName("null 검색어이면 입력 순서를 유지해서 전체 후보를 반환한다")
        void success_Search_NullKeywordBoundary() {
            // given
            SearchTarget first = target("김관리", "first@mapo.go.kr");
            SearchTarget second = target("이관리", "second@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(
                    List.of(first, second),
                    null
            );

            // then
            assertThat(result).containsExactly(first, second);
        }

        @Test
        @DisplayName("후보 목록이 비어 있으면 빈 결과를 반환한다")
        void success_Search_EmptyCandidatesBoundary() {
            // given
            List<SearchTarget> candidates = List.of();

            // when
            List<SearchTarget> result = matcher.search(candidates, "김관리");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("정확 일치, 부분 일치, 오타 일치 순서로 정렬한다")
        void success_Search_RelevanceOrder() {
            // given
            SearchTarget typo = target("이학준", "dlgkrwns213@korea.kr");
            SearchTarget contains = target("김관리", "admin-dkkkr@mapo.go.kr");
            SearchTarget exact = target("dkkkr", "other@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(
                    List.of(typo, contains, exact),
                    "dkkkr"
            );

            // then
            assertThat(result).containsExactly(exact, contains, typo);
        }

        @Test
        @DisplayName("접두어 일치를 일반 부분 일치보다 먼저 정렬한다")
        void success_Search_PrefixBeforeContains() {
            // given
            SearchTarget contains = target("김관리", "my-admin@mapo.go.kr");
            SearchTarget prefix = target("이관리", "admin-user@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(
                    List.of(contains, prefix),
                    "admin"
            );

            // then
            assertThat(result).containsExactly(prefix, contains);
        }

        @Test
        @DisplayName("검색 점수가 같으면 기존 조회 순서를 유지한다")
        void success_Search_StableOrder() {
            // given
            SearchTarget first = target("김가", "first@mapo.go.kr");
            SearchTarget second = target("김나", "second@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(
                    List.of(first, second),
                    "김"
            );

            // then
            assertThat(result).containsExactly(first, second);
        }

        @Test
        @DisplayName("관련 없는 이름과 이메일은 검색 결과에서 제외한다")
        void success_Search_NoMatchBoundary() {
            // given
            SearchTarget target = target("김관리", "manager@mapo.go.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "zzzzz");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("오타 유사도가 임계값보다 낮으면 검색 결과에서 제외한다")
        void success_Search_TypoThresholdBoundary() {
            // given
            SearchTarget target = target("김관리", "abxyz@korea.kr");

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "abcde");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이름과 이메일 값이 없어도 검색을 안전하게 처리한다")
        void success_Search_NullCandidateValuesBoundary() {
            // given
            SearchTarget target = target(null, null);

            // when
            List<SearchTarget> result = matcher.search(List.of(target), "admin");

            // then
            assertThat(result).isEmpty();
        }
    }

    private SearchTarget target(String name, String email) {
        return new SearchTarget(name, email);
    }

    private record SearchTarget(
            String name,
            String email
    ) implements AdminNameEmailSearchTarget {
    }
}
