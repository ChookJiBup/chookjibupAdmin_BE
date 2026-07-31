package com.example.chookjibupadmin.api.fieldstaff.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateFieldStaffResponseTest {

    @Test
    @DisplayName("생성 완료 화면에 필요한 계정 정보와 임시 비밀번호를 응답한다")
    void success_From() {
        // given
        LocalDateTime validFrom = LocalDateTime.of(2026, 10, 9, 0, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 10, 18, 23, 59);
        FieldStaffAccount account = FieldStaffAccount.create(
                1L,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                validFrom,
                validUntil
        );
        CreateFieldStaffResult result =
                new CreateFieldStaffResult(account, "TempPass123!");

        // when
        CreateFieldStaffResponse response = CreateFieldStaffResponse.from(result);

        // then
        assertThat(response.staffId()).isEqualTo(account.getPublicId());
        assertThat(response.loginId()).isEqualTo("staff01");
        assertThat(response.name()).isEqualTo("김스태프");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.validFrom()).isEqualTo(validFrom);
        assertThat(response.validUntil()).isEqualTo(validUntil);
        assertThat(response.temporaryPassword()).isEqualTo("TempPass123!");
    }
}
