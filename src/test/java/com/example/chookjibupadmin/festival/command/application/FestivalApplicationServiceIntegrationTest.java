package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.command.application.dto.UpdateFestivalCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Autowired
    private FestivalLocationService festivalLocationService;

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
            assertThat(festivalLocationService.findAllByFestivalId(foundFestival.getId()))
                    .extracting(location -> location.getLocationName())
                    .containsExactly("월드컵공원", "임시 주차장");
        }

        @Test
        @DisplayName("대표 장소가 둘이면 축제 생성을 거절한다")
        void fail_Create_DuplicatedPrimaryLocation() {
            AdminAccount admin = adminAccountService.save(unassignedAdmin());
            CreateFestivalCommand valid = createCommand();
            FestivalLocationCommand second = valid.locations().get(1);
            CreateFestivalCommand invalid = new CreateFestivalCommand(
                    valid.seriesId(),
                    valid.name(),
                    valid.description(),
                    List.of(
                            valid.locations().getFirst(),
                            new FestivalLocationCommand(
                                    second.locationType(),
                                    second.locationName(),
                                    second.roadAddress(),
                                    second.jibunAddress(),
                                    second.detailAddress(),
                                    second.postalCode(),
                                    second.buildingManagementNumber(),
                                    second.latitude(),
                                    second.longitude(),
                                    true,
                                    second.sortOrder()
                            )
                    ),
                    valid.startDate(),
                    valid.endDate(),
                    valid.operationStartTime(),
                    valid.operationEndTime()
            );
            assertThatThrownBy(() -> festivalApplicationService.create(invalid, principal(admin)))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("장소 주소를 수정해도 장소 UUID를 유지한다")
        void success_Update_PreservesLocationPublicIds() {
            AdminAccount admin = adminAccountService.save(unassignedAdmin());
            Festival festival =
                    festivalApplicationService.create(createCommand(), principal(admin));
            List<FestivalLocation> before =
                    festivalLocationService.findAllByFestivalId(festival.getId());
            Set<java.util.UUID> publicIds =
                    before.stream().map(FestivalLocation::getPublicId).collect(Collectors.toSet());

            UpdateFestivalCommand command =
                    new UpdateFestivalCommand(
                            festival.getNameValue(),
                            "수정된 축제 설명",
                            List.of(
                                    locationCommand(before.get(0), "서울특별시 마포구 새 주소 1"),
                                    locationCommand(before.get(1), "서울특별시 마포구 새 주소 2")
                            ),
                            LocalDate.of(2026, 10, 16),
                            LocalDate.of(2026, 10, 18),
                            LocalTime.of(10, 0),
                            LocalTime.of(21, 0)
                    );

            festivalApplicationService.update(festival.getPublicId(), command, principal(admin));

            List<FestivalLocation> after =
                    festivalLocationService.findAllByFestivalId(festival.getId());
            assertThat(after)
                    .extracting(FestivalLocation::getPublicId)
                    .containsExactlyInAnyOrderElementsOf(publicIds);
            assertThat(after)
                    .extracting(FestivalLocation::getRoadAddress)
                    .containsExactly("서울특별시 마포구 새 주소 1", "서울특별시 마포구 새 주소 2");
        }
    }

    private CreateFestivalCommand createCommand() {
        return new CreateFestivalCommand(
                null,
                "마포나루 새우젓축제",
                "마포구 대표 지역 축제",
                List.of(
                        new FestivalLocationCommand(
                                FestivalLocationType.MAIN_VENUE,
                                "월드컵공원",
                                "서울특별시 마포구 월드컵로 243",
                                null,
                                "중앙광장",
                                null,
                                null,
                                null,
                                null,
                                true,
                                0
                        ),
                        new FestivalLocationCommand(
                                FestivalLocationType.PARKING,
                                "임시 주차장",
                                "서울특별시 마포구 성산동 123",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false,
                                1
                        )
                ),
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }

    private FestivalLocationCommand locationCommand(FestivalLocation location, String roadAddress) {
        return new FestivalLocationCommand(
                location.getPublicId(),
                location.getLocationType(),
                location.getLocationName(),
                roadAddress,
                location.getJibunAddress(),
                location.getDetailAddress(),
                location.getPostalCode(),
                location.getBuildingManagementNumber(),
                location.getLatitude(),
                location.getLongitude(),
                location.getBoundaryGeometry(),
                location.isPrimary(),
                location.getSortOrder()
        );
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
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
