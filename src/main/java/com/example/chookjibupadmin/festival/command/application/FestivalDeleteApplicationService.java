package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalDeletionTarget;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 축제의 외부 파일 삭제와 DB 영구 삭제를 순서대로 조정한다.
 */
@Service
@RequiredArgsConstructor
public class FestivalDeleteApplicationService {

    private final FestivalDeletionLifecycleService lifecycleService;
    private final MapImageStoragePort mapImageStoragePort;

    public void delete(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        FestivalDeletionTarget target = lifecycleService.beginDeletion(
                festivalPublicId,
                principal
        );
        target.objectKeys().forEach(mapImageStoragePort::delete);
        lifecycleService.completeDeletion(festivalPublicId, principal);
    }
}
