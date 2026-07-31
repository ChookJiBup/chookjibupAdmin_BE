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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FieldStaffAccountRepositoryImplTest {

    @InjectMocks
    private FieldStaffAccountRepositoryImpl repository;

    @Mock
    private FieldStaffAccountJpaRepository jpaRepository;

    @Test
    @DisplayName("외부 UUID 목록 조회를 JPA 저장소에 위임한다")
    void success_FindAllByPublicIdIn() {
        // given
        FieldStaffAccount account = fieldStaffAccount();
        List<UUID> publicIds = List.of(account.getPublicId());
        given(jpaRepository.findAllByPublicIdIn(publicIds))
                .willReturn(List.of(account));

        // when
        List<FieldStaffAccount> result = repository.findAllByPublicIdIn(publicIds);

        // then
        assertThat(result).containsExactly(account);
        then(jpaRepository).should().findAllByPublicIdIn(publicIds);
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
