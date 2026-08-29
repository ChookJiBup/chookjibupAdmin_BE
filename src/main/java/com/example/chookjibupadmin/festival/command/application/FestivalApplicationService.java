package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.command.application.dto.UpdateFestivalCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisQueueApplicationService;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.command.application.dto.UploadedFestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 기본 정보 생성과 수정 유스케이스를 조정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalApplicationService {

    /** 대한민국 인근 허용 박스 — 위도 (국경 Polygon 아님). */
    private static final BigDecimal KOREA_LAT_MIN = new BigDecimal("33.0");
    private static final BigDecimal KOREA_LAT_MAX = new BigDecimal("38.7");
    /** 대한민국 인근 허용 박스 — 경도. */
    private static final BigDecimal KOREA_LNG_MIN = new BigDecimal("124.5");
    private static final BigDecimal KOREA_LNG_MAX = new BigDecimal("132.0");

    private final FestivalService festivalService;
    private final FestivalSeriesService festivalSeriesService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalMapService festivalMapService;
    private final MapAnalysisQueueApplicationService mapAnalysisQueueService;
    private final FestivalLocationService festivalLocationService;
    private final FestivalVisitorCountService visitorCountService;

    /**
     * 축제 묶음을 연결한 뒤 연도별 축제 기본 정보를 저장하고 생성자를 1관리자로 배정한다.
     */
    public Festival create(
            CreateFestivalCommand command,
            AdminPrincipal principal
    ) {
        return createFestival(
                command,
                principal,
                UUID.randomUUID(),
                null
        ).festival();
    }

    /**
     * S3 저장이 완료된 최초 배치도와 축제 기본 정보를 한 트랜잭션으로 저장한다.
     */
    public CreateFestivalWithMapResult createWithMap(
            CreateFestivalCommand command,
            AdminPrincipal principal,
            UUID festivalPublicId,
            UploadedFestivalMap uploadedMap
    ) {
        if (uploadedMap == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return createFestival(
                command,
                principal,
                festivalPublicId,
                uploadedMap
        );
    }

    private CreateFestivalWithMapResult createFestival(
            CreateFestivalCommand command,
            AdminPrincipal principal,
            UUID festivalPublicId,
            UploadedFestivalMap uploadedMap
    ) {
        AdminAccount creator = findAuthenticatedAdmin(principal);
        if (!creator.canCreateFestival()) {
            throw new CustomException(ErrorCode.AUTH_FESTIVAL_CREATE_FORBIDDEN);
        }
        if (uploadedMap != null && !creator.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        FestivalName name = FestivalName.of(command.name());
        FestivalPeriod period = FestivalPeriod.of(
                command.startDate(),
                command.endDate()
        );
        FestivalSeries series = findOrCreateSeries(command.seriesId(), name);
        validateSeriesName(series, name);
        validateUniqueFestivalYear(series.getId(), period.getStartDate().getYear());
        FestivalLocationCommand primaryLocation = validateLocations(command.locations());

        Festival festival = Festival.create(
                festivalPublicId,
                series.getId(),
                series.getPublicId(),
                name,
                FestivalDescription.of(command.description()),
                FestivalAddress.of(representativeAddress(primaryLocation)),
                FestivalDetailAddress.of(primaryLocation.detailAddress()),
                period,
                FestivalOperationTime.of(
                        command.operationStartTime(),
                        command.operationEndTime()
                ),
                normalizeCreateVisitorMode(command.visitorCountInputMode())
        );

        Festival savedFestival = festivalService.save(festival);
        List<FestivalLocation> savedLocations = festivalLocationService.saveAll(
                toLocations(savedFestival, command.locations(), creator.getId())
        );
        adminFestivalRoleService.assignFestivalOwner(
                creator.getId(),
                savedFestival.getId()
        );

        FestivalMap festivalMap = null;
        MapAnalysisJob analysisJob = null;
        if (uploadedMap != null) {
            FestivalMap uploadedFestivalMap = FestivalMap.uploaded(
                    uploadedMap.publicId(),
                    savedFestival.getId(),
                    uploadedMap.mapName(),
                    uploadedMap.originalFileName(),
                    uploadedMap.originalImageKey(),
                    uploadedMap.displayImageKey(),
                    uploadedMap.analysisImageKey(),
                    uploadedMap.originalContentType(),
                    uploadedMap.displayContentType(),
                    uploadedMap.analysisContentType(),
                    uploadedMap.originalFileSize(),
                    uploadedMap.displayFileSize(),
                    uploadedMap.analysisFileSize(),
                    uploadedMap.displayImageDimensions(),
                    uploadedMap.analysisImageDimensions(),
                    uploadedMap.originalChecksumSha256(),
                    uploadedMap.displayChecksumSha256(),
                    uploadedMap.analysisChecksumSha256(),
                    creator.getId()
            );
            FestivalLocation primary = savedLocations.stream()
                    .filter(FestivalLocation::isPrimary)
                    .findFirst()
                    .orElseThrow();
            uploadedFestivalMap.assignLocation(primary.getId());
            festivalMap = festivalMapService.save(uploadedFestivalMap);
            analysisJob = mapAnalysisQueueService.enqueueInitial(festivalMap);
        }

        return new CreateFestivalWithMapResult(savedFestival, festivalMap, analysisJob);
    }

    private void validateSeriesName(
            FestivalSeries series,
            FestivalName name
    ) {
        if (!series.getNormalizedName().equals(FestivalSeries.normalize(name))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private FestivalSeries findOrCreateSeries(
            UUID seriesId,
            FestivalName name
    ) {
        if (seriesId != null) {
            return festivalSeriesService.getByPublicId(seriesId);
        }

        String normalizedName = FestivalSeries.normalize(name);
        return festivalSeriesService.findByNormalizedName(normalizedName)
                .orElseGet(() -> festivalSeriesService.save(
                        FestivalSeries.create(name)
                ));
    }

    private void validateUniqueFestivalYear(
            Long seriesId,
            int year
    ) {
        if (festivalService.existsBySeriesIdAndYear(seriesId, year)) {
            throw new CustomException(ErrorCode.FESTIVAL_YEAR_ALREADY_EXISTS);
        }
    }

    /**
     * 1관리자 권한으로 담당 축제의 기본 정보를 수정한다.
     */
    public Festival update(
            UUID festivalId,
            UpdateFestivalCommand command,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicIdForUpdate(festivalId);
        validateFestivalOwner(festival, adminAccount);
        FestivalLocationCommand primaryLocation = validateLocations(command.locations());
        festival.updateBasicInfo(
                FestivalName.of(command.name()),
                FestivalDescription.of(command.description()),
                FestivalAddress.of(representativeAddress(primaryLocation)),
                FestivalDetailAddress.of(primaryLocation.detailAddress()),
                FestivalPeriod.of(command.startDate(), command.endDate()),
                FestivalOperationTime.of(
                        command.operationStartTime(),
                        command.operationEndTime()
                )
        );
        if (command.visitorCountInputMode() != null) {
            FestivalVisitorCountInputMode nextMode =
                    requireSelectableVisitorMode(command.visitorCountInputMode());
            if (festival.getVisitorCountInputMode() != nextMode) {
                ensureVisitorModeChangeAllowed(festival.getId());
                festival.changeVisitorCountInputMode(nextMode);
            }
        }
        synchronizeLocations(festival, command.locations(), adminAccount.getId());

        return festival;
    }

    private void ensureVisitorModeChangeAllowed(Long festivalId) {
        boolean hasDaily = !visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festivalId)
                .isEmpty();
        boolean hasTotal = visitorCountService
                .findTotalByFestivalId(festivalId)
                .isPresent();
        if (hasDaily || hasTotal) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_VISITOR_INPUT_MODE_CHANGE_FORBIDDEN
            );
        }
    }

    private FestivalVisitorCountInputMode normalizeCreateVisitorMode(
            FestivalVisitorCountInputMode mode
    ) {
        if (mode == null) {
            return FestivalVisitorCountInputMode.UNSET;
        }
        return requireSelectableVisitorMode(mode);
    }

    private FestivalVisitorCountInputMode requireSelectableVisitorMode(
            FestivalVisitorCountInputMode mode
    ) {
        if (mode == FestivalVisitorCountInputMode.UNSET) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return mode;
    }

    private FestivalLocationCommand validateLocations(List<FestivalLocationCommand> locations) {
        if (locations == null
                || locations.isEmpty()
                || locations.size() > 100
                || locations.stream().anyMatch(location -> location == null)
                || locations.stream().filter(FestivalLocationCommand::primary).count() != 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        HashSet<String> keys = new HashSet<>();
        HashSet<UUID> locationIds = new HashSet<>();
        for (FestivalLocationCommand location : locations) {
            if (location.locationId() != null
                    && !locationIds.add(location.locationId())) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            String key = String.join(
                    "|",
                    normalize(location.buildingManagementNumber()),
                    normalize(location.roadAddress()),
                    normalize(location.jibunAddress()),
                    normalize(location.detailAddress()),
                    normalize(location.locationName())
            );
            if (!keys.add(key)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            validateLocationCoordinates(location);
        }
        return locations.stream()
                .filter(FestivalLocationCommand::primary)
                .findFirst()
                .orElseThrow();
    }

    /**
     * 대표 장소는 위경도 필수. 보조 장소는 둘 다 null 허용.
     * 좌표가 있으면 대한민국 인근 허용 박스 안에 있어야 한다.
     */
    private void validateLocationCoordinates(FestivalLocationCommand location) {
        BigDecimal lat = location.latitude();
        BigDecimal lng = location.longitude();
        boolean missingLat = lat == null;
        boolean missingLng = lng == null;
        if (location.primary()) {
            if (missingLat || missingLng) {
                throw new CustomException(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED);
            }
        } else if (missingLat != missingLng) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        } else if (missingLat) {
            return;
        }
        if (isOutsideKorea(lat, lng)) {
            throw new CustomException(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA);
        }
    }

    private static boolean isOutsideKorea(BigDecimal lat, BigDecimal lng) {
        return lat.compareTo(KOREA_LAT_MIN) < 0
                || lat.compareTo(KOREA_LAT_MAX) > 0
                || lng.compareTo(KOREA_LNG_MIN) < 0
                || lng.compareTo(KOREA_LNG_MAX) > 0;
    }

    private List<FestivalLocation> toLocations(
            Festival festival,
            List<FestivalLocationCommand> commands,
            Long adminId
    ) {
        return commands.stream()
                .map(location -> FestivalLocation.create(
                        festival,
                        location.locationType(),
                        location.locationName(),
                        location.roadAddress(),
                        location.jibunAddress(),
                        location.detailAddress(),
                        location.postalCode(),
                        location.buildingManagementNumber(),
                        location.latitude(),
                        location.longitude(),
                        location.boundaryGeometry(),
                        location.primary(),
                        location.sortOrder(),
                        adminId
                ))
                .toList();
    }

    private void synchronizeLocations(
            Festival festival,
            List<FestivalLocationCommand> commands,
            Long adminId
    ) {
        List<FestivalLocation> existing = festivalLocationService.findAllByFestivalId(
                festival.getId()
        );
        Map<UUID, FestivalLocation> byPublicId = existing.stream()
                .collect(Collectors.toMap(
                        FestivalLocation::getPublicId,
                        Function.identity()
                ));
        List<FestivalLocation> synchronizedLocations = commands.stream()
                .map(command -> {
                    if (command.locationId() == null) {
                        return FestivalLocation.create(
                                festival,
                                command.locationType(),
                                command.locationName(),
                                command.roadAddress(),
                                command.jibunAddress(),
                                command.detailAddress(),
                                command.postalCode(),
                                command.buildingManagementNumber(),
                                command.latitude(),
                                command.longitude(),
                                command.boundaryGeometry(),
                                command.primary(),
                                command.sortOrder(),
                                adminId
                        );
                    }
                    FestivalLocation location = byPublicId.remove(command.locationId());
                    if (location == null) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST);
                    }
                    location.update(
                            command.locationType(),
                            command.locationName(),
                            command.roadAddress(),
                            command.jibunAddress(),
                            command.detailAddress(),
                            command.postalCode(),
                            command.buildingManagementNumber(),
                            command.latitude(),
                            command.longitude(),
                            command.boundaryGeometry(),
                            command.primary(),
                            command.sortOrder(),
                            adminId
                    );
                    return location;
                })
                .toList();
        List<FestivalLocation> removals = List.copyOf(byPublicId.values());
        if (removals.stream()
                .anyMatch(location -> festivalMapService.existsByLocationId(location.getId()))) {
            throw new CustomException(ErrorCode.FESTIVAL_LOCATION_IN_USE);
        }
        festivalLocationService.deleteAll(removals);
        festivalLocationService.saveAll(synchronizedLocations);
    }

    private String representativeAddress(FestivalLocationCommand location) {
        String address = location.roadAddress();
        if (address == null || address.isBlank()) {
            address = location.jibunAddress();
        }
        if (address == null || address.isBlank()) {
            address = location.locationName();
        }
        return address;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private void validateFestivalOwner(
            Festival festival,
            AdminAccount adminAccount
    ) {
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccount.getId(),
                        festival.getId()
                );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }
}
