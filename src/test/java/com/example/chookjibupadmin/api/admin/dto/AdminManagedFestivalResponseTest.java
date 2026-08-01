package com.example.chookjibupadmin.api.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminManagedFestivalResponseTest {

    @Test
    @DisplayName("관리 축제 조회 결과를 상세주소가 포함된 응답으로 변환한다")
    void success_From() {
        // given
        AdminManagedFestivalView view = new AdminManagedFestivalView(
                UUID.randomUUID(),
                "광주비엔날레",
                2026,
                AdminRole.FESTIVAL_OWNER,
                FestivalStatus.DRAFT,
                FestivalProgressStatus.ONGOING,
                "광주광역시 북구 비엔날레로 111",
                "광주비엔날레 전시관",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 11, 15)
        );

        // when
        AdminManagedFestivalResponse response =
                AdminManagedFestivalResponse.from(view);

        // then
        assertThat(response.address()).isEqualTo(view.address());
        assertThat(response.detailAddress()).isEqualTo(view.detailAddress());
        assertThat(response.progressStatus()).isEqualTo(view.progressStatus());
    }
}
