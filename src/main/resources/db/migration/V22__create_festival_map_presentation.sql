-- 카카오맵 표시 설정: 부지 경계 폴리곤 + 팜플렛 오버레이(지도 교체와 분리된 표시용 이미지).
-- festival_maps.anchor_* 는 AI 투영 기준이고, 이 테이블의 overlay 앵커는 표시만 조정한다.
CREATE TABLE IF NOT EXISTS festival_map_presentation (
    id                          BIGSERIAL PRIMARY KEY,
    map_id                      BIGINT NOT NULL,
    festival_id                 BIGINT NOT NULL,
    boundary_geometry           JSONB,
    overlay_image_key           VARCHAR(512),
    overlay_asset_id            UUID,
    overlay_image_width         INTEGER,
    overlay_image_height        INTEGER,
    overlay_center_lat          NUMERIC(10, 7),
    overlay_center_lng          NUMERIC(10, 7),
    overlay_ground_width_m      NUMERIC(10, 2),
    overlay_rotation_deg        NUMERIC(6, 3),
    overlay_opacity             NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    overlay_visible             BOOLEAN NOT NULL DEFAULT FALSE,
    clip_to_boundary            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_festival_map_presentation_map UNIQUE (map_id)
);

ALTER TABLE festival_map_presentation
    DROP CONSTRAINT IF EXISTS chk_festival_map_presentation_overlay_anchor_complete;

ALTER TABLE festival_map_presentation
    ADD CONSTRAINT chk_festival_map_presentation_overlay_anchor_complete CHECK (
        (
            overlay_center_lat IS NULL
                AND overlay_center_lng IS NULL
                AND overlay_ground_width_m IS NULL
                AND overlay_rotation_deg IS NULL
            )
            OR (
            overlay_center_lat IS NOT NULL
                AND overlay_center_lng IS NOT NULL
                AND overlay_ground_width_m IS NOT NULL
                AND overlay_rotation_deg IS NOT NULL
                AND overlay_center_lat BETWEEN -90 AND 90
                AND overlay_center_lng BETWEEN -180 AND 180
                AND overlay_ground_width_m > 0
            )
        );

ALTER TABLE festival_map_presentation
    DROP CONSTRAINT IF EXISTS chk_festival_map_presentation_opacity;

ALTER TABLE festival_map_presentation
    ADD CONSTRAINT chk_festival_map_presentation_opacity CHECK (
        overlay_opacity >= 0 AND overlay_opacity <= 1
    );

ALTER TABLE festival_map_presentation
    DROP CONSTRAINT IF EXISTS chk_festival_map_presentation_clip_requires_boundary;

ALTER TABLE festival_map_presentation
    ADD CONSTRAINT chk_festival_map_presentation_clip_requires_boundary CHECK (
        clip_to_boundary = FALSE OR boundary_geometry IS NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_festival_map_presentation_festival
    ON festival_map_presentation (festival_id);
