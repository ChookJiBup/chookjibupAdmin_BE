package com.example.demoadmin.admin.command.domain;

import com.example.demoadmin.common.domain.BaseTimeEntity;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import jakarta.persistence.Column;
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
 * 관리자 계정이 특정 축제에서 갖는 역할을 표현한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "admin_festival_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_festival_roles_public_id",
                        columnNames = "public_id"
                ),
                @UniqueConstraint(
                        name = "uk_admin_festival_roles_admin_festival",
                        columnNames = {"admin_account_id", "festival_id"}
                )
        }
)
public class AdminFestivalRole extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "admin_account_id", nullable = false)
    private Long adminAccountId;

    @Column(name = "festival_id", nullable = false)
    private Long festivalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminRole role;

    @Column(name = "invited_by_admin_id")
    private Long invitedByAdminId;

    private AdminFestivalRole(
            Long adminAccountId,
            Long festivalId,
            AdminRole role,
            Long invitedByAdminId
    ) {
        if (adminAccountId == null || festivalId == null || role == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.publicId = UUID.randomUUID();
        this.adminAccountId = adminAccountId;
        this.festivalId = festivalId;
        this.role = role;
        this.invitedByAdminId = invitedByAdminId;
    }

    /**
     * 축제 생성자를 해당 축제의 1관리자로 배정한다.
     */
    public static AdminFestivalRole createFestivalOwner(
            Long adminAccountId,
            Long festivalId
    ) {
        return new AdminFestivalRole(
                adminAccountId,
                festivalId,
                AdminRole.FESTIVAL_OWNER,
                null
        );
    }

    /**
     * 1관리자가 초대한 관리자를 해당 축제의 2관리자로 배정한다.
     */
    public static AdminFestivalRole createSubAdmin(
            Long adminAccountId,
            Long festivalId,
            Long invitedByAdminId
    ) {
        if (invitedByAdminId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return new AdminFestivalRole(
                adminAccountId,
                festivalId,
                AdminRole.SUB_ADMIN,
                invitedByAdminId
        );
    }

    public boolean canInviteSubAdmin() {
        return role.canInviteSubAdmin();
    }

    public boolean canModifyFestivalInfo() {
        return role.canModifyFestivalInfo();
    }

    public boolean canManageFieldStaff() {
        return role.canManageFieldStaff();
    }

    public boolean canManageQueueDesign() {
        return role.canManageQueueDesign();
    }

    public boolean canViewOperationReport() {
        return role.canViewOperationReport();
    }

    public boolean canUpdateQueueTail() {
        return role.canUpdateQueueTail();
    }
}
