package com.example.chookjibupadmin.festival.location.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class FestivalLocationJpaRepositoryTest {

    @Autowired
    private FestivalLocationJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("축제 장소를 정렬 순서와 ID 순으로 조회한다")
    void success_FindAllByFestivalId_InSortOrder() {
        Festival festival = persistFestival("첫 번째 축제", 2026);
        repository.save(location(festival, "주차장", 2, false));
        repository.save(location(festival, "중앙광장", 0, true));

        assertThat(repository.findAllByFestival_IdOrderBySortOrderAscIdAsc(festival.getId()))
                .extracting(FestivalLocation::getLocationName)
                .containsExactly("중앙광장", "주차장");
    }

    @Test
    @DisplayName("축제를 삭제하면 같은 도메인의 장소도 cascade 삭제한다")
    void success_DeleteFestival_CascadesLocations() {
        Festival festival = persistFestival("삭제할 축제", 2026);
        repository.saveAndFlush(location(festival, "중앙광장", 0, true));

        entityManager.remove(festival);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByFestival_IdOrderBySortOrderAscIdAsc(festival.getId()))
                .isEmpty();
    }

    private FestivalLocation location(Festival festival, String name, int order, boolean primary) {
        return FestivalLocation.create(
                festival,
                primary ? FestivalLocationType.MAIN_VENUE : FestivalLocationType.PARKING,
                name,
                "서울특별시 마포구 " + name,
                null,
                null,
                null,
                null,
                null,
                null,
                primary,
                order,
                1L
        );
    }

    private Festival persistFestival(String name, int year) {
        Festival festival =
                Festival.create(
                        (long) name.hashCode(),
                        UUID.randomUUID(),
                        FestivalName.of(name),
                        FestivalDescription.of("지역 축제 설명"),
                        FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                        FestivalDetailAddress.of("월드컵공원"),
                        FestivalPeriod.of(LocalDate.of(year, 10, 16), LocalDate.of(year, 10, 18)),
                        FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0))
                );
        entityManager.persist(festival);
        entityManager.flush();
        return festival;
    }
}
