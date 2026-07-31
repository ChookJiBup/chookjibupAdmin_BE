package com.example.chookjibupadmin.operator.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class FieldStaffAccountJpaRepositoryTest {

    @Autowired
    private FieldStaffAccountJpaRepository jpaRepository;

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("외부 UUID로 현장 스태프 계정을 조회한다")
        void success_FindByPublicId() {
            // given
            FieldStaffAccount saved = jpaRepository.save(fieldStaffAccount("staff01"));

            // when
            var result = jpaRepository.findByPublicId(saved.getPublicId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getLoginIdValue()).isEqualTo("staff01");
        }
    }

    @Nested
    @DisplayName("findByFestivalIdAndLoginId")
    class FindByFestivalIdAndLoginId {

        @Test
        @DisplayName("축제 ID와 로그인 아이디로 현장 스태프 계정을 조회한다")
        void success_FindByFestivalIdAndLoginId() {
            // given
            jpaRepository.save(fieldStaffAccount("staff01"));

            // when
            var result = jpaRepository.findByFestivalIdAndLoginId(
                    1L,
                    FieldStaffLoginId.of("STAFF01")
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getNameValue()).isEqualTo("김스태프");
        }
    }

    @Nested
    @DisplayName("existsByFestivalIdAndLoginId")
    class ExistsByFestivalIdAndLoginId {

        @Test
        @DisplayName("축제 안에서 로그인 아이디 존재 여부를 확인한다")
        void success_ExistsByFestivalIdAndLoginId() {
            // given
            jpaRepository.save(fieldStaffAccount("staff01"));

            // when
            boolean exists = jpaRepository.existsByFestivalIdAndLoginId(
                    1L,
                    FieldStaffLoginId.of("staff01")
            );

            // then
            assertThat(exists).isTrue();
        }
    }

    private FieldStaffAccount fieldStaffAccount(String loginId) {
        return FieldStaffAccount.create(
                1L,
                FieldStaffLoginId.of(loginId),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
    }
}
