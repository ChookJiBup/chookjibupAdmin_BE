package com.example.demoadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.admin.command.domain.AdminRole;
import com.example.demoadmin.admin.command.domain.vo.AdminEmail;
import com.example.demoadmin.admin.command.domain.vo.AdminName;
import com.example.demoadmin.admin.command.domain.vo.AdminOrganization;
import com.example.demoadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.demoadmin.festival.command.domain.Festival;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FestivalApplicationServiceIntegrationTest {

    @Autowired
    private FestivalApplicationService festivalApplicationService;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private FestivalSeriesService festivalSeriesService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("축제를 저장하고 생성자를 1관리자로 배정한다")
        void success_Create_AssignFestivalOwner() {
            // given
            AdminAccount adminAccount = adminAccountService.save(
                    unassignedAdmin()
            );
            CreateFestivalCommand command = createCommand();

            // when
            Festival festival = festivalApplicationService.create(
                    command,
                    principal(adminAccount)
            );

            // then
            Festival foundFestival = festivalService.getById(festival.getId());
            AdminFestivalRole role =
                    adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                            adminAccount.getId(),
                            foundFestival.getId()
                    );
            assertThat(foundFestival.getNameValue()).isEqualTo(command.name());
            assertThat(foundFestival.getDetailAddressValue())
                    .isEqualTo(command.detailAddress());
            assertThat(foundFestival.getSeriesId()).isNotNull();
            assertThat(foundFestival.getYear()).isEqualTo(2026);
            assertThat(festivalSeriesService.getById(foundFestival.getSeriesId()))
                    .isNotNull();
            assertThat(role.getRole()).isEqualTo(AdminRole.FESTIVAL_OWNER);
        }
    }

    private CreateFestivalCommand createCommand() {
        return new CreateFestivalCommand(
                null,
                "마포나루 새우젓축제",
                "마포구 대표 지역 축제",
                "서울특별시 마포구 월드컵로 243",
                "월드컵공원",
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getFestivalId(),
                adminAccount.getEmailValue(),
                adminAccount.getRole()
        );
    }

    private AdminAccount unassignedAdmin() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
