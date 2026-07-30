package com.example.demoadmin.booth.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoothQueryApplicationServiceTest {

    @InjectMocks
    private BoothQueryApplicationService boothQueryApplicationService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private BoothQueryService boothQueryService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("getQueueLines")
    class GetQueueLines {

        @Test
        @DisplayName("Command Entity 없이 대기 라인 projection을 조회한다")
        void success_GetQueueLines() {
            // given
            Festival festival = festival();
            UUID boothId = UUID.randomUUID();
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            BoothView boothView = org.mockito.Mockito.mock(BoothView.class);
            List<BoothQueueLineView> expected = List.of(
                    org.mockito.Mockito.mock(BoothQueueLineView.class)
            );
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    principal.adminId(),
                    festival.getId()
            )).willReturn(AdminFestivalRole.createFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            ));
            given(boothQueryService.getByFestivalIdAndPublicId(
                    festival.getId(),
                    boothId
            )).willReturn(boothView);
            given(boothQueryService.findQueueLinesByFestivalIdAndBoothPublicId(
                    festival.getId(),
                    boothId
            )).willReturn(expected);

            // when
            List<BoothQueueLineView> result =
                    boothQueryApplicationService.getQueueLines(
                            festival.getPublicId(),
                            boothId,
                            principal
                    );

            // then
            assertThat(result).isEqualTo(expected);
            then(boothQueryService).should().getByFestivalIdAndPublicId(
                    festival.getId(),
                    boothId
            );
        }
    }

    private Festival festival() {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("김밥축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
        ReflectionTestUtils.setField(festival, "id", 1L);
        return festival;
    }
}
