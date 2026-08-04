package com.example.chookjibupadmin.map.command.infrastructure.storage;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * S3 설정 전에도 애플리케이션을 기동하되 업로드 요청은 명확히 거절한다.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.map.storage",
        name = "provider",
        havingValue = "disabled",
        matchIfMissing = true
)
public class DisabledMapImageStorageAdapter implements MapImageStoragePort {

    @Override
    public void upload(StoredMapImageFile imageFile) {
        throw new CustomException(ErrorCode.FESTIVAL_MAP_STORAGE_NOT_CONFIGURED);
    }

    @Override
    public void delete(String objectKey) {
        throw new CustomException(ErrorCode.FESTIVAL_MAP_STORAGE_NOT_CONFIGURED);
    }

    @Override
    public MapImageReadUrl createReadUrl(String objectKey) {
        throw new CustomException(ErrorCode.FESTIVAL_MAP_STORAGE_NOT_CONFIGURED);
    }

    @Override
    public byte[] read(String objectKey, long maxBytes) {
        throw new CustomException(ErrorCode.FESTIVAL_MAP_STORAGE_NOT_CONFIGURED);
    }
}
