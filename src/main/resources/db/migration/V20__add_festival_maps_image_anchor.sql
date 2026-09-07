-- 배치도 이미지를 실세계 위경도에 고정하는 앵커.
-- 네 컬럼이 모두 채워져야 이미지 정규화 좌표(schema 1.0)를 WGS84(schema 2.0)로 옮길 수 있다.
-- 기존 지도는 앵커가 없으므로 nullable로 추가하고, 앵커가 없으면 예전처럼 1.0으로 남는다.
ALTER TABLE festival_maps
    ADD COLUMN IF NOT EXISTS anchor_center_lat NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS anchor_center_lng NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS anchor_ground_width_m NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS anchor_rotation_deg NUMERIC(6, 3);

ALTER TABLE festival_maps
    DROP CONSTRAINT IF EXISTS chk_festival_maps_anchor_complete;

-- 부분적으로만 채워진 앵커는 투영을 조용히 어긋나게 하므로 애초에 막는다.
ALTER TABLE festival_maps
    ADD CONSTRAINT chk_festival_maps_anchor_complete CHECK (
        (
            anchor_center_lat IS NULL
                AND anchor_center_lng IS NULL
                AND anchor_ground_width_m IS NULL
                AND anchor_rotation_deg IS NULL
            )
            OR (
            anchor_center_lat IS NOT NULL
                AND anchor_center_lng IS NOT NULL
                AND anchor_ground_width_m IS NOT NULL
                AND anchor_rotation_deg IS NOT NULL
                AND anchor_center_lat BETWEEN -90 AND 90
                AND anchor_center_lng BETWEEN -180 AND 180
                AND anchor_ground_width_m > 0
            )
        );
