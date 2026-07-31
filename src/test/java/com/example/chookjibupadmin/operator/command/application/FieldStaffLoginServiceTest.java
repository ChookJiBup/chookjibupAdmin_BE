package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginCommand;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FieldStaffLoginServiceTest {

    @InjectMocks
    private FieldStaffLoginService service;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FieldStaffTokenProvider tokenProvider;

    @Mock
    private Clock clock;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("유효기간 안의 현장 스태프 로그인에 성공한다")
        void success_Login() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            FieldStaffLoginCommand command = command(festival.getPublicId(), "staff01", "plain");
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getByFestivalIdAndLoginIdForLogin(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(account);
            given(passwordEncoder.matches("plain", "encoded-password")).willReturn(true);
            given(clock.instant()).willReturn(Instant.parse("2026-10-10T00:00:00Z"));
            given(clock.getZone()).willReturn(ZoneId.of("UTC"));
            given(tokenProvider.createAccessToken(account)).willReturn("token");
            given(tokenProvider.getAccessTokenExpirationSeconds()).willReturn(1800L);

            // when
            FieldStaffLoginResult result = service.login(command);

            // then
            assertThat(result.accessToken()).isEqualTo("token");
            assertThat(result.fieldStaffAccount()).isSameAs(account);
            assertThat(result.festivalPublicId()).isEqualTo(festival.getPublicId());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 로그인할 수 없다")
        void fail_Login_InvalidCredentials_CustomException() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            FieldStaffLoginCommand command = command(festival.getPublicId(), "staff01", "wrong");
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getByFestivalIdAndLoginIdForLogin(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(account);
            given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> service.login(command))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("유효기간 전이면 로그인할 수 없다")
        void fail_Login_ValidPeriodExpired_CustomException() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            FieldStaffLoginCommand command = command(festival.getPublicId(), "staff01", "plain");
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getByFestivalIdAndLoginIdForLogin(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(account);
            given(passwordEncoder.matches("plain", "encoded-password")).willReturn(true);
            given(clock.instant()).willReturn(Instant.parse("2026-10-08T23:59:59Z"));
            given(clock.getZone()).willReturn(ZoneId.of("UTC"));

            // when & then
            assertThatThrownBy(() -> service.login(command))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_VALID_PERIOD_EXPIRED.getMessage());
        }

        @Test
        @DisplayName("삭제된 계정은 로그인할 수 없다")
        void fail_Login_FieldStaffNotActive_CustomException() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            account.delete();
            FieldStaffLoginCommand command = command(festival.getPublicId(), "staff01", "plain");
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getByFestivalIdAndLoginIdForLogin(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(account);
            given(passwordEncoder.matches("plain", "encoded-password")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> service.login(command))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_ACTIVE.getMessage());
        }
    }

    private FieldStaffLoginCommand command(
            UUID festivalId,
            String loginId,
            String password
    ) {
        return new FieldStaffLoginCommand(festivalId, loginId, password);
    }

    private Festival festival(Long festivalId) {
        Festival festival = Festival.create(
                10L,
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
        ReflectionTestUtils.setField(festival, "id", festivalId);
        return festival;
    }

    private FieldStaffAccount fieldStaffAccount(Long festivalId) {
        FieldStaffAccount account = FieldStaffAccount.create(
                festivalId,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDate.of(2026, 10, 9).atStartOfDay(),
                LocalDate.of(2026, 10, 18).atTime(LocalTime.MAX)
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }
}
