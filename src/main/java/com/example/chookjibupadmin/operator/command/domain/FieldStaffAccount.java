package com.example.chookjibupadmin.operator.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 현장에서 제한 권한으로 사용하는 스태프 계정 Aggregate이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "field_staff_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_field_staff_accounts_public_id",
                        columnNames = "public_id"
                ),
                @UniqueConstraint(
                        name = "uk_field_staff_accounts_festival_login_id",
                        columnNames = {"festival_id", "login_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_field_staff_accounts_festival_status_id",
                        columnList = "festival_id, status, id"
                ),
                @Index(
                        name = "idx_field_staff_accounts_festival_public_status",
                        columnList = "festival_id, public_id, status"
                )
        }
)
public class FieldStaffAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO(operator): 운영 DB 반영 전 field_staff_accounts 마이그레이션을 작성한다.
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "login_id", nullable = false, length = 30)
    )
    private FieldStaffLoginId loginId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "name", nullable = false, length = 100)
    )
    private FieldStaffName name;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "phone_number", nullable = false, length = 20)
    )
    private FieldStaffPhoneNumber phoneNumber;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "password_hash", nullable = false, length = 255)
    )
    private FieldStaffPasswordHash passwordHash;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FieldStaffStatus status;

    private FieldStaffAccount(
            Long festivalId,
            FieldStaffLoginId loginId,
            FieldStaffName name,
            FieldStaffPhoneNumber phoneNumber,
            FieldStaffPasswordHash passwordHash,
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
        validateFestivalId(festivalId);
        validateValidPeriod(validFrom, validUntil);

        this.publicId = UUID.randomUUID();
        this.festivalId = festivalId;
        this.loginId = loginId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.status = FieldStaffStatus.ACTIVE;
    }

    /**
     * 축제 기간에 종속되는 현장 스태프 계정을 생성한다.
     */
    public static FieldStaffAccount create(
            Long festivalId,
            FieldStaffLoginId loginId,
            FieldStaffName name,
            FieldStaffPhoneNumber phoneNumber,
            FieldStaffPasswordHash passwordHash,
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
        return new FieldStaffAccount(
                festivalId,
                loginId,
                name,
                phoneNumber,
                passwordHash,
                validFrom,
                validUntil
        );
    }

    /**
     * 삭제된 계정인지 확인한다.
     */
    public boolean isDeleted() {
        return status == FieldStaffStatus.DELETED;
    }

    /**
     * 관리자가 활성화한 계정인지 확인한다.
     */
    public boolean isActive() {
        return status == FieldStaffStatus.ACTIVE;
    }

    /**
     * 현재 시점에 로그인 가능한 계정인지 확인한다.
     */
    public boolean isUsableAt(LocalDateTime now) {
        return isActive() && isWithinValidPeriod(now);
    }

    /**
     * 지정 시각이 계정 유효기간 안인지 확인한다.
     */
    public boolean isWithinValidPeriod(LocalDateTime now) {
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    /**
     * 현장 스태프 계정을 삭제 상태로 변경한다.
     */
    public void delete() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_ACTIVE);
        }

        status = FieldStaffStatus.DELETED;
    }

    public void update(
            FieldStaffName name,
            FieldStaffPhoneNumber phoneNumber
    ) {
        ensureNotDeleted();
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public void changePassword(FieldStaffPasswordHash passwordHash) {
        ensureNotDeleted();
        this.passwordHash = passwordHash;
    }

    public void activate() {
        ensureNotDeleted();
        status = FieldStaffStatus.ACTIVE;
    }

    public void deactivate() {
        ensureNotDeleted();
        status = FieldStaffStatus.INACTIVE;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_ACTIVE);
        }
    }

    public String getLoginIdValue() {
        return loginId.getValue();
    }

    public String getNameValue() {
        return name.getValue();
    }

    public String getPhoneNumberValue() {
        return phoneNumber.getValue();
    }

    public String getPasswordHashValue() {
        return passwordHash.getValue();
    }

    private static void validateFestivalId(Long festivalId) {
        if (festivalId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateValidPeriod(
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
        if (validFrom == null || validUntil == null || validFrom.isAfter(validUntil)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
