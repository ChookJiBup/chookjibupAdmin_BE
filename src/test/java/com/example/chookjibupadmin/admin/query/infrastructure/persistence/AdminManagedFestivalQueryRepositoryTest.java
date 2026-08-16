package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.admin.query.repository.AdminManagedFestivalQueryRepository;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({AdminManagedFestivalQueryRepositoryImpl.class, QuerydslConfig.class})
class AdminManagedFestivalQueryRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 1);

    private long seriesId = 1L;

    @Autowired
    private AdminManagedFestivalQueryRepository queryRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("searchCurrentManagedFestivals")
    class SearchCurrentManagedFestivals {

        @Test
        @DisplayName("현재 관리 중인 축제를 조회한다")
        void success_SearchCurrentManagedFestivals() {
            // given
            Festival festival = persist(festival("마포나루 새우젓축제", 2026));
            AdminAccount owner = persistOwner("owner@mapo.go.kr", festival);
            persist(admin("plain@mapo.go.kr"));

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    owner.getId(),
                    new AdminManagedFestivalCondition(null, null, null),
                    TODAY
            );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalName)
                    .containsExactly("마포나루 새우젓축제");
            assertThat(result.getFirst().detailAddress())
                    .isEqualTo("월드컵공원");
        }

        @Test
        @DisplayName("역할, 연도, 검색어 조건으로 현재 관리 축제를 필터링한다")
        void success_SearchCurrentManagedFestivals_ByCondition() {
            // given
            Festival festival = persist(festival("마포나루 새우젓축제", 2026));
            AdminAccount owner = persistOwner("owner@mapo.go.kr", festival);

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    owner.getId(),
                    new AdminManagedFestivalCondition(
                            AdminRole.FESTIVAL_OWNER,
                            2026,
                            "새우젓"
                    ),
                    TODAY
            );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalYear)
                    .containsExactly(2026);
        }

        @Test
        @DisplayName("보조 장소 주소 검색어로도 축제를 중복 없이 조회한다")
        void success_SearchCurrentManagedFestivals_BySecondaryLocation() {
            Festival festival = persist(festival("다중 장소 축제", 2026));
            AdminAccount owner = persistOwner("multi@mapo.go.kr", festival);
            persist(
                    FestivalLocation.create(
                            festival,
                            FestivalLocationType.PARKING,
                            "임시 주차장",
                            "서울특별시 은평구 통일로 1",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            1,
                            owner.getId()
                    ));
            var result =
                    queryRepository.searchCurrentManagedFestivals(
                            owner.getId(),
                            new AdminManagedFestivalCondition(null, null, "은평구"),
                            TODAY
                    );
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalName)
                    .containsExactly("다중 장소 축제");
        }

        @Test
        @DisplayName("조건에 맞는 관리 축제가 없으면 빈 목록을 반환한다")
        void success_SearchCurrentManagedFestivals_EmptyBoundary() {
            // given
            Festival festival = persist(festival("마포나루 새우젓축제", 2026));
            AdminAccount owner = persistOwner("owner@mapo.go.kr", festival);

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    owner.getId(),
                    new AdminManagedFestivalCondition(
                            AdminRole.SUB_ADMIN,
                            2026,
                            null
                    ),
                    TODAY
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("진행 상태 조건으로 현재 관리 축제를 필터링한다")
        void success_SearchCurrentManagedFestivals_ByProgressStatus() {
            // given
            AdminAccount adminAccount = persist(admin("admin@mapo.go.kr"));
            Festival upcoming = persist(festival(
                    "진행 예정 축제",
                    TODAY.plusDays(1),
                    TODAY.plusDays(3)
            ));
            Festival ongoing = persist(festival(
                    "진행 중 축제",
                    TODAY.minusDays(1),
                    TODAY.plusDays(1)
            ));
            persist(AdminFestivalRole.createFestivalOwner(
                    adminAccount.getId(),
                    upcoming.getId()
            ));
            persist(AdminFestivalRole.createFestivalOwner(
                    adminAccount.getId(),
                    ongoing.getId()
            ));

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    adminAccount.getId(),
                    new AdminManagedFestivalCondition(
                            null,
                            null,
                            null,
                            FestivalProgressStatus.ONGOING
                    ),
                    TODAY
            );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalName)
                    .containsExactly("진행 중 축제");
            assertThat(result)
                    .extracting(AdminManagedFestivalView::progressStatus)
                    .containsExactly(FestivalProgressStatus.ONGOING);
        }

        @Test
        @DisplayName("제2 관리자는 배정된 축제를 자신의 역할로 조회한다")
        void success_SearchCurrentManagedFestivals_SubAdmin() {
            // given
            AdminAccount owner = persist(admin("owner@mapo.go.kr"));
            AdminAccount subAdmin = persist(admin("sub@mapo.go.kr"));
            Festival festival = persist(festival(
                    "제2 관리자 축제",
                    TODAY.minusDays(1),
                    TODAY.plusDays(1)
            ));
            persist(AdminFestivalRole.createSubAdmin(
                    subAdmin.getId(),
                    festival.getId(),
                    owner.getId()
            ));

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    subAdmin.getId(),
                    new AdminManagedFestivalCondition(null, null, null),
                    TODAY
            );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::role)
                    .containsExactly(AdminRole.SUB_ADMIN);
        }

        @Test
        @DisplayName("진행 상태와 운영 우선순위에 따라 정렬한다")
        void success_SearchCurrentManagedFestivals_ProgressStatusOrder() {
            // given
            AdminAccount adminAccount = persist(admin("admin@mapo.go.kr"));
            Festival completedOld = persist(festival(
                    "완료 오래된 축제",
                    TODAY.minusDays(10),
                    TODAY.minusDays(5)
            ));
            Festival upcomingLater = persist(festival(
                    "예정 늦은 축제",
                    TODAY.plusDays(5),
                    TODAY.plusDays(6)
            ));
            Festival ongoingSoon = persist(festival(
                    "진행 곧 종료 축제",
                    TODAY.minusDays(1),
                    TODAY.plusDays(1)
            ));
            Festival completedRecent = persist(festival(
                    "완료 최근 축제",
                    TODAY.minusDays(4),
                    TODAY.minusDays(1)
            ));
            Festival upcomingSoon = persist(festival(
                    "예정 가까운 축제",
                    TODAY.plusDays(1),
                    TODAY.plusDays(2)
            ));
            for (Festival festival : List.of(
                    completedOld,
                    upcomingLater,
                    ongoingSoon,
                    completedRecent,
                    upcomingSoon
            )) {
                persist(AdminFestivalRole.createFestivalOwner(
                        adminAccount.getId(),
                        festival.getId()
                ));
            }

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    adminAccount.getId(),
                    new AdminManagedFestivalCondition(null, null, null),
                    TODAY
            );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalName)
                    .containsExactly(
                            "예정 가까운 축제",
                            "예정 늦은 축제",
                            "진행 곧 종료 축제",
                            "완료 최근 축제",
                            "완료 오래된 축제"
                    );
        }
    }

    @Nested
    @DisplayName("findCurrentManagedFestival")
    class FindCurrentManagedFestival {

        @Test
        @DisplayName("현재 관리 중인 축제를 UUID로 조회한다")
        void success_FindCurrentManagedFestival() {
            // given
            Festival festival = persist(festival("마포나루 새우젓축제", 2026));
            AdminAccount owner = persistOwner("owner@mapo.go.kr", festival);

            // when
            var result = queryRepository.findCurrentManagedFestival(
                    owner.getId(),
                    festival.getPublicId(),
                    TODAY
            );

            // then
            assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(AdminManagedFestivalView::festivalId)
                    .isEqualTo(festival.getPublicId());
            assertThat(result.get().progressStatus())
                    .isEqualTo(FestivalProgressStatus.UPCOMING);
        }

        @Test
        @DisplayName("관리하지 않는 축제는 조회 결과가 없다")
        void success_FindCurrentManagedFestival_NotManagedBoundary() {
            // given
            Festival managed = persist(festival("마포나루 새우젓축제", 2026));
            Festival notManaged = persist(festival("서울빛초롱축제", 2027));
            AdminAccount owner = persistOwner("owner@mapo.go.kr", managed);

            // when
            var result = queryRepository.findCurrentManagedFestival(
                    owner.getId(),
                    notManaged.getPublicId(),
                    TODAY
            );

            // then
            assertThat(result).isEmpty();
        }
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    private AdminAccount persistOwner(String email, Festival festival) {
        AdminAccount owner = persist(admin(email));
        persist(AdminFestivalRole.createFestivalOwner(
                owner.getId(),
                festival.getId()
        ));
        return owner;
    }

    private AdminAccount admin(String email) {
        return AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of("김관리"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private Festival festival(String name, int year) {
        return festival(
                name,
                LocalDate.of(year, 10, 16),
                LocalDate.of(year, 10, 18)
        );
    }

    private Festival festival(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Festival.create(
                seriesId++,
                UUID.randomUUID(),
                FestivalName.of(name),
                FestivalDescription.of("지역 축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(
                        startDate,
                        endDate
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
