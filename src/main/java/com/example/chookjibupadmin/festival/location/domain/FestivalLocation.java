package com.example.chookjibupadmin.festival.location.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

/**
 * 축제 회차에 속한 실제 장소와 운영 권역을 관리한다.
 */
@Entity
@Getter
@Table(
        name = "festival_locations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_festival_locations_public_id",
                        columnNames = "public_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_festival_locations_festival",
                        columnList = "festival_id"
                ),
                @Index(
                        name = "idx_festival_locations_source_primary",
                        columnList = "festival_id,source_type,is_primary"
                )
        },
        check = {
                @CheckConstraint(
                        name = "chk_festival_location_coordinates",
                        constraint =
                                """
                                        (latitude is null and longitude is null)
                                        or (latitude is not null and longitude is not null)
                                        """
                ),
                @CheckConstraint(
                        name = "chk_festival_location_latitude",
                        constraint = "latitude is null or (latitude >= -90 and latitude <= 90)"
                ),
                @CheckConstraint(
                        name = "chk_festival_location_longitude",
                        constraint = "longitude is null or (longitude >= -180 and longitude <= 180)"
                ),
                @CheckConstraint(
                        name = "chk_festival_location_sort_order",
                        constraint = "sort_order >= 0"
                ),
                @CheckConstraint(
                        name = "chk_festival_location_geography",
                        constraint =
                                """
                                        road_address is not null
                                        or jibun_address is not null
                                        or latitude is not null
                                        or boundary_geometry is not null
                                        """
                ),
                @CheckConstraint(
                        name = "chk_festival_location_source_admin",
                        constraint =
                                """
                                        (source_type = 'API' and created_by_admin_id is null)
                                        or (source_type = 'MANUAL' and created_by_admin_id is not null)
                                        """
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalLocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "festival_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_festival_locations_festival")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Festival festival;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 30)
    private FestivalLocationType locationType;

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "building_management_number", length = 30)
    private String buildingManagementNumber;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_geometry", columnDefinition = "jsonb")
    private Map<String, Object> boundaryGeometry;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private FestivalLocationSourceType sourceType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by_admin_id", updatable = false)
    private Long createdByAdminId;

    @Column(name = "last_modified_by_admin_id")
    private Long lastModifiedByAdminId;

    /**
     * 관리자가 입력한 장소를 생성한다.
     */
    public static FestivalLocation create(
            Festival festival,
            FestivalLocationType type,
            String name,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String postalCode,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            Map<String, Object> boundaryGeometry,
            boolean primary,
            int sortOrder,
            Long adminId
    ) {
        validate(
                festival,
                type,
                name,
                roadAddress,
                jibunAddress,
                detailAddress,
                postalCode,
                buildingManagementNumber,
                latitude,
                longitude,
                boundaryGeometry,
                sortOrder,
                adminId
        );
        FestivalLocation location = new FestivalLocation();
        location.publicId = UUID.randomUUID();
        location.festival = festival;
        location.locationType = type;
        location.locationName = name.trim();
        location.roadAddress = trimToNull(roadAddress);
        location.jibunAddress = trimToNull(jibunAddress);
        location.detailAddress = trimToNull(detailAddress);
        location.postalCode = trimToNull(postalCode);
        location.buildingManagementNumber = trimToNull(buildingManagementNumber);
        location.latitude = latitude;
        location.longitude = longitude;
        location.boundaryGeometry = copy(boundaryGeometry);
        location.sourceType = FestivalLocationSourceType.MANUAL;
        location.primary = primary;
        location.sortOrder = sortOrder;
        location.createdByAdminId = adminId;
        location.lastModifiedByAdminId = adminId;
        return location;
    }

    public static FestivalLocation create(
            Festival festival,
            FestivalLocationType type,
            String name,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String postalCode,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean primary,
            int sortOrder,
            Long adminId
    ) {
        return create(
                festival,
                type,
                name,
                roadAddress,
                jibunAddress,
                detailAddress,
                postalCode,
                buildingManagementNumber,
                latitude,
                longitude,
                null,
                primary,
                sortOrder,
                adminId
        );
    }

    /**
     * 장소의 공개 UUID와 생성 출처를 유지하면서 관리 정보만 변경한다.
     */
    public void update(
            FestivalLocationType type,
            String name,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String postalCode,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            Map<String, Object> boundaryGeometry,
            boolean primary,
            int sortOrder,
            Long adminId
    ) {
        validate(
                festival,
                type,
                name,
                roadAddress,
                jibunAddress,
                detailAddress,
                postalCode,
                buildingManagementNumber,
                latitude,
                longitude,
                boundaryGeometry,
                sortOrder,
                adminId
        );
        this.locationType = type;
        this.locationName = name.trim();
        this.roadAddress = trimToNull(roadAddress);
        this.jibunAddress = trimToNull(jibunAddress);
        this.detailAddress = trimToNull(detailAddress);
        this.postalCode = trimToNull(postalCode);
        this.buildingManagementNumber = trimToNull(buildingManagementNumber);
        this.latitude = latitude;
        this.longitude = longitude;
        this.boundaryGeometry = copy(boundaryGeometry);
        this.primary = primary;
        this.sortOrder = sortOrder;
        this.lastModifiedByAdminId = adminId;
    }

    private static void validate(
            Festival festival,
            FestivalLocationType type,
            String name,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String postalCode,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            Map<String, Object> boundaryGeometry,
            int sortOrder,
            Long adminId
    ) {
        if (festival == null
                || type == null
                || adminId == null
                || name == null
                || name.isBlank()
                || name.trim().length() > 150
                || length(roadAddress) > 255
                || length(jibunAddress) > 255
                || length(detailAddress) > 100
                || length(postalCode) > 10
                || length(buildingManagementNumber) > 30
                || sortOrder < 0
                || (latitude == null) != (longitude == null)
                || lacksGeography(roadAddress, jibunAddress, latitude, boundaryGeometry)
                || isOutOfRange(latitude, -90, 90)
                || isOutOfRange(longitude, -180, 180)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static boolean lacksGeography(
            String roadAddress,
            String jibunAddress,
            BigDecimal latitude,
            Map<String, Object> boundaryGeometry
    ) {
        return blank(roadAddress)
                && blank(jibunAddress)
                && latitude == null
                && (boundaryGeometry == null || boundaryGeometry.isEmpty());
    }

    private static boolean isOutOfRange(BigDecimal value, int min, int max) {
        return value != null
                && (value.compareTo(BigDecimal.valueOf(min)) < 0
                || value.compareTo(BigDecimal.valueOf(max)) > 0);
    }

    private static int length(String value) {
        return value == null ? 0 : value.trim().length();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null ? null : Map.copyOf(value);
    }
}
