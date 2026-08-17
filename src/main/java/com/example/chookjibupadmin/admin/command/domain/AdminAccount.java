package com.example.chookjibupadmin.admin.command.domain;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
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

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "organization", nullable = false, length = 255)
    )
    private AdminOrganization organization;

    // TODO(admin): 운영 DB 반영 전 기존 계정의 부서와 직급을 백필하는 마이그레이션을 작성한다.
    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "department", nullable = false, length = 100)
    )
    private AdminDepartment department;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "job_rank", nullable = false, length = 50)
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
            AdminEmail email,
            AdminName name,
            AdminOrganization organization,
            AdminDepartment department,
            AdminRank rank,
            AdminPasswordHash passwordHash
    ) {
        if (department == null || rank == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.publicId = UUID.randomUUID();
        this.email = email;
        this.name = name;
        this.organization = organization;
        this.department = department;
        this.rank = rank;
        this.passwordHash = passwordHash;
        this.authVersion = 0L;
        this.status = AdminStatus.ACTIVE;
    }

    /**
     * 아직 축제를 만들거나 초대받지 않은 일반 관리자 계정을 생성한다.
     */
    public static AdminAccount createAdmin(
            AdminEmail email,
            AdminName name,
            AdminOrganization organization,
            AdminDepartment department,
            AdminRank rank,
            AdminPasswordHash passwordHash
    ) {
        return new AdminAccount(
                email,
                name,
                organization,
                department,
                rank,
                passwordHash
        );
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

    /** 관리자 본인의 프로필 정보를 변경한다. */
    public void updateProfile(
            AdminName name,
            AdminDepartment department,
            AdminRank rank
    ) {
        if (name == null || department == null || rank == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.name = name;
        this.department = department;
        this.rank = rank;
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

    public String getDepartmentValue() {
        return department.getValue();
    }

    public String getRankValue() {
        return rank.getValue();
    }

    public String getPasswordHashValue() {
        return passwordHash.getValue();
    }
}
