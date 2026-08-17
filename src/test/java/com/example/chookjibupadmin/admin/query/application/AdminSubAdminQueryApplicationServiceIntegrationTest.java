package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminSubAdminQueryApplicationServiceIntegrationTest {

    @Autowired
    private AdminSubAdminQueryApplicationService applicationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Nested
    @DisplayName("getSubAdmins")
    class GetSubAdmins {

        @Test
        @DisplayName("제1 관리자가 담당 축제의 서브관리자 목록을 조회한다")
        void success_GetSubAdmins_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            persistSubAdmin(
                    "sub1@mapo.go.kr",
                    festival,
                    owner
            );
            persistSubAdmin(
                    "sub2@mapo.go.kr",
                    festival,
                    owner
            );

            // when
            List<AdminSubAdminView> result = applicationService.getSubAdmins(
                    festival.getPublicId(),
                    null,
                    principal(owner)
            );

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("김관리");
            assertThat(result.get(0).email()).isEqualTo("sub1@mapo.go.kr");
            assertThat(result.get(1).name()).isEqualTo("김관리");
            assertThat(result.get(1).email()).isEqualTo("sub2@mapo.go.kr");
        }

        @Test
        @DisplayName("제1 관리자가 이메일 오타로 등록된 운영자를 검색한다")
        void success_GetSubAdmins_EmailTypo() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            persistSubAdmin(
                    "dlgkrwns213@korea.kr",
                    festival,
                    owner
            );
            persistSubAdmin(
                    "other@korea.kr",
                    festival,
                    owner
            );

            // when
            List<AdminSubAdminView> result = applicationService.getSubAdmins(
                    festival.getPublicId(),
                    "dkkkr",
                    principal(owner)
            );

            // then
            assertThat(result).singleElement().satisfies(subAdmin -> {
                assertThat(subAdmin.name()).isEqualTo("김관리");
                assertThat(subAdmin.email()).isEqualTo("dlgkrwns213@korea.kr");
            });
        }

        @Test
        @DisplayName("제1 관리자가 이름으로 등록된 운영자를 검색한다")
        void success_GetSubAdmins_Name() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            persistSubAdmin(
                    "first@korea.kr",
                    "김검색",
                    festival,
                    owner
            );
            persistSubAdmin(
                    "second@korea.kr",
                    "이관리",
                    festival,
                    owner
            );

            // when
            List<AdminSubAdminView> result = applicationService.getSubAdmins(
                    festival.getPublicId(),
                    "김검색",
                    principal(owner)
            );

            // then
            assertThat(result).singleElement().satisfies(subAdmin -> {
                assertThat(subAdmin.name()).isEqualTo("김검색");
                assertThat(subAdmin.email()).isEqualTo("first@korea.kr");
            });
        }
    }

    @Nested
    @DisplayName("getSubAdmin")
    class GetSubAdmin {

        @Test
        @DisplayName("제1 관리자가 담당 축제의 서브관리자를 UUID로 조회한다")
        void success_GetSubAdmin_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            AdminAccount subAdmin = persistSubAdmin(
                    "sub@mapo.go.kr",
                    festival,
                    owner
            );

            // when
            AdminSubAdminView result = applicationService.getSubAdmin(
                    festival.getPublicId(),
                    subAdmin.getPublicId(),
                    principal(owner)
            );

            // then
            assertThat(result.adminId()).isEqualTo(subAdmin.getPublicId());
            assertThat(result.email()).isEqualTo("sub@mapo.go.kr");
        }
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private AdminAccount persistOwner(Festival festival) {
        AdminAccount owner = adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
        return owner;
    }

    private AdminAccount persistSubAdmin(
            String email,
            Festival festival,
            AdminAccount owner
    ) {
        return persistSubAdmin(email, "김관리", festival, owner);
    }

    private AdminAccount persistSubAdmin(
            String email,
            String name,
            Festival festival,
            AdminAccount owner
    ) {
        AdminAccount subAdmin = adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignSubAdmin(
                subAdmin.getId(),
                festival.getId(),
                owner.getId()
        );
        return subAdmin;
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
