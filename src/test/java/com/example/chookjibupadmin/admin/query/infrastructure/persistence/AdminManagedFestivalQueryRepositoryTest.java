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
import com.example.chookjibupadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
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
            AdminAccount owner = persist(owner(
                    "owner@mapo.go.kr",
                    festival.getId()
            ));
            persist(admin("plain@mapo.go.kr"));

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    owner.getId(),
                    new AdminManagedFestivalCondition(null, null, null)
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
            AdminAccount owner = persist(owner(
                    "owner@mapo.go.kr",
                    festival.getId()
            ));

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    owner.getId(),
                    new AdminManagedFestivalCondition(
                            AdminRole.FESTIVAL_OWNER,
                            2026,
                            "새우젓"
                    )
            );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalYear)
                    .containsExactly(2026);
        }

        @Test
        @DisplayName("조건에 맞는 관리 축제가 없으면 빈 목록을 반환한다")
        void success_SearchCurrentManagedFestivals_EmptyBoundary() {
            // given
            Festival festival = persist(festival("마포나루 새우젓축제", 2026));
            AdminAccount owner = persist(owner(
                    "owner@mapo.go.kr",
                    festival.getId()
            ));

            // when
            var result = queryRepository.searchCurrentManagedFestivals(
                    owner.getId(),
                    new AdminManagedFestivalCondition(
                            AdminRole.SUB_ADMIN,
                            2026,
                            null
                    )
            );

            // then
            assertThat(result).isEmpty();
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
            AdminAccount owner = persist(owner(
                    "owner@mapo.go.kr",
                    festival.getId()
            ));

            // when
            var result = queryRepository.findCurrentManagedFestival(
                    owner.getId(),
                    festival.getPublicId()
            );

            // then
            assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(AdminManagedFestivalView::festivalId)
                    .isEqualTo(festival.getPublicId());
        }

        @Test
        @DisplayName("관리하지 않는 축제는 조회 결과가 없다")
        void success_FindCurrentManagedFestival_NotManagedBoundary() {
            // given
            Festival managed = persist(festival("마포나루 새우젓축제", 2026));
            Festival notManaged = persist(festival("서울빛초롱축제", 2027));
            AdminAccount owner = persist(owner(
                    "owner@mapo.go.kr",
                    managed.getId()
            ));

            // when
            var result = queryRepository.findCurrentManagedFestival(
                    owner.getId(),
                    notManaged.getPublicId()
            );

            // then
            assertThat(result).isEmpty();
        }
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        if (entity instanceof AdminAccount adminAccount
                && adminAccount.getFestivalId() != null
                && adminAccount.getRole() == AdminRole.FESTIVAL_OWNER) {
            entityManager.persist(AdminFestivalRole.createFestivalOwner(
                    adminAccount.getId(),
                    adminAccount.getFestivalId()
            ));
            entityManager.flush();
        }
        return entity;
    }

    private AdminAccount owner(String email, Long festivalId) {
        return AdminAccount.createFestivalOwner(
                AdminEmail.of(email),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                festivalId,
                AdminPasswordHash.of("encoded-password")
        );
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
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of(name),
                FestivalDescription.of("지역 축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(
                        LocalDate.of(year, 10, 16),
                        LocalDate.of(year, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
