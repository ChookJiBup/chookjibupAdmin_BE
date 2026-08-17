package com.example.chookjibupadmin.admin.command.domain;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 계정 Aggregate이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "admin_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_accounts_public_id",
                        columnNames = "public_id"
                ),
                @UniqueConstraint(
                        name = "uk_admin_accounts_email",
                        columnNames = "email"
                )
        }
)
public class AdminAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO(admin): 운영 DB 반영 전 기존 admin_accounts 데이터의 public_id 백필 마이그레이션을 작성한다.
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_kind", nullable = false, length = 50)
    private AccountKind accountKind;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email", nullable = false, length = 255)
    )
    private AdminEmail email;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "name", nullable = false, length = 100)
    )
    private AdminName name;

    /** 과·팀 또는 외부업자 업체명 */
    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "organization", nullable = false, length = 255)
    )
    private AdminOrganization organization;

    /** 직급 (예: 과장, 주무관). 외부업자는 null */
    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "job_rank", length = 50)
    )
    private AdminRank rank;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "password_hash", nullable = false, length = 255)
    )
    private AdminPasswordHash passwordHash;

    @Column(
            name = "auth_version",
            nullable = false,
            columnDefinition = "bigint default 0"
    )
    private long authVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminStatus status;

    private AdminAccount(
            AccountKind accountKind,
            AdminEmail email,
            AdminName name,
            AdminOrganization organization,
            AdminRank rank,
            AdminPasswordHash passwordHash
    ) {
        if (accountKind == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (accountKind == AccountKind.GOVERNMENT && rank == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.publicId = UUID.randomUUID();
        this.accountKind = accountKind;
        this.email = email;
        this.name = name;
        this.organization = organization;
        this.rank = rank;
        this.passwordHash = passwordHash;
        this.authVersion = 0L;
        this.status = AdminStatus.ACTIVE;
    }

    /**
     * 공무원 관리자 계정을 생성한다.
     */
    public static AdminAccount createGovernment(
            AdminEmail email,
            AdminName name,
            AdminOrganization organization,
            AdminRank rank,
            AdminPasswordHash passwordHash
    ) {
        return new AdminAccount(
                AccountKind.GOVERNMENT,
                email,
                name,
                organization,
                rank,
                passwordHash
        );
    }

    /**
     * 외부업자 관리자 계정을 생성한다.
     */
    public static AdminAccount createContractor(
            AdminEmail email,
            AdminName name,
            AdminOrganization companyName,
            AdminPasswordHash passwordHash
    ) {
        return new AdminAccount(
                AccountKind.CONTRACTOR,
                email,
                name,
                companyName,
                null,
                passwordHash
        );
    }

    /**
     * @deprecated {@link #createGovernment} 사용
     */
    @Deprecated
    public static AdminAccount createAdmin(
            AdminEmail email,
            AdminName name,
            AdminOrganization organization,
            AdminRank rank,
            AdminPasswordHash passwordHash
    ) {
        return createGovernment(email, name, organization, rank, passwordHash);
    }

    /**
     * 축제를 새로 생성할 수 있는 계정인지 확인한다.
     */
    public boolean canCreateFestival() {
        return accountKind == AccountKind.GOVERNMENT;
    }

    /**
     * 외부업자(운영자) 계정인지 확인한다.
     */
    public boolean isContractor() {
        return accountKind == AccountKind.CONTRACTOR;
    }

    /**
     * 로그인과 API 사용이 가능한 활성 계정인지 확인한다.
     */
    public boolean isActive() {
        return status.canAuthenticate();
    }

    /**
     * 탈퇴 처리된 계정인지 확인한다.
     */
    public boolean isDeleted() {
        return status == AdminStatus.DELETED;
    }

    /**
     * 관리자 계정을 탈퇴 상태로 변경한다.
     */
    public void withdraw() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_ALREADY_WITHDRAWN);
        }

        status = AdminStatus.DELETED;
    }

    /**
     * 비밀번호를 변경하고 기존에 발급된 모든 관리자 JWT를 무효화한다.
     */
    public void changePassword(AdminPasswordHash passwordHash) {
        if (passwordHash == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.passwordHash = passwordHash;
        this.authVersion++;
    }

    /** 공무원 본인의 프로필 정보를 변경한다. */
    public void updateGovernmentProfile(
            AdminName name,
            AdminOrganization organization,
            AdminRank rank
    ) {
        if (accountKind != AccountKind.GOVERNMENT) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (name == null || organization == null || rank == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.name = name;
        this.organization = organization;
        this.rank = rank;
    }

    /** 외부업자 본인의 프로필 정보를 변경한다. */
    public void updateContractorProfile(
            AdminName name,
            AdminOrganization companyName
    ) {
        if (accountKind != AccountKind.CONTRACTOR) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (name == null || companyName == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.name = name;
        this.organization = companyName;
    }

    /**
     * @deprecated {@link #updateGovernmentProfile} 또는 {@link #updateContractorProfile} 사용
     */
    @Deprecated
    public void updateProfile(
            AdminName name,
            AdminOrganization organization,
            AdminRank rank
    ) {
        updateGovernmentProfile(name, organization, rank);
    }

    public String getEmailValue() {
        return email.getValue();
    }

    public String getNameValue() {
        return name.getValue();
    }

    public String getOrganizationValue() {
        return organization.getValue();
    }

    public String getRankValue() {
        return rank == null ? null : rank.getValue();
    }

    public String getPasswordHashValue() {
        return passwordHash.getValue();
    }
}
