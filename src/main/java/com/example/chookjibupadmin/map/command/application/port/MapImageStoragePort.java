package com.example.chookjibupadmin.map.command.application.port;

import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;

/**
 * 배치도 이미지 객체 저장소 계약이다.
 */
public interface MapImageStoragePort {

    void upload(StoredMapImageFile imageFile);

    void delete(String objectKey);

    MapImageReadUrl createReadUrl(String objectKey);

    byte[] read(String objectKey, long maxBytes);
}
