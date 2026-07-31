package com.example.chookjibupadmin.operator.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FieldStaffAccountRepositoryTest {

    @InjectMocks
    private FieldStaffAccountRepositoryImpl repository;

    @Mock
    private FieldStaffAccountJpaRepository jpaRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("현장 스태프 계정을 저장한다")
        void success_Save() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            given(jpaRepository.save(account)).willReturn(account);

            // when
            FieldStaffAccount saved = repository.save(account);

            // then
            assertThat(saved).isSameAs(account);
            then(jpaRepository).should().save(account);
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("외부 UUID로 현장 스태프 계정을 조회한다")
        void success_FindByPublicId() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            given(jpaRepository.findByPublicId(account.getPublicId()))
                    .willReturn(Optional.of(account));

            // when
            var result = repository.findByPublicId(account.getPublicId());

            // then
            assertThat(result).contains(account);
        }
    }

    @Nested
    @DisplayName("findByFestivalIdAndLoginId")
    class FindByFestivalIdAndLoginId {

        @Test
        @DisplayName("축제와 로그인 아이디로 현장 스태프 계정을 조회한다")
        void success_FindByFestivalIdAndLoginId() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            FieldStaffLoginId loginId = FieldStaffLoginId.of("staff01");
            given(jpaRepository.findByFestivalIdAndLoginId(1L, loginId))
                    .willReturn(Optional.of(account));

            // when
            var result = repository.findByFestivalIdAndLoginId(1L, loginId);

            // then
            assertThat(result).contains(account);
        }
    }

    @Nested
    @DisplayName("existsByFestivalIdAndLoginId")
    class ExistsByFestivalIdAndLoginId {

        @Test
        @DisplayName("축제 안에서 로그인 아이디 존재 여부를 확인한다")
        void success_ExistsByFestivalIdAndLoginId() {
            // given
            FieldStaffLoginId loginId = FieldStaffLoginId.of("staff01");
            given(jpaRepository.existsByFestivalIdAndLoginId(1L, loginId))
                    .willReturn(true);

            // when
            boolean exists = repository.existsByFestivalIdAndLoginId(1L, loginId);

            // then
            assertThat(exists).isTrue();
        }
    }

    private FieldStaffAccount fieldStaffAccount() {
        return FieldStaffAccount.create(
                1L,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
    }
}
