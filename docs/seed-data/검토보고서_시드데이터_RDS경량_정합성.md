문서 형식: 검토보고서

# 시드 데이터 RDS 경량 — MD·SQL 정합성 검토

작성일: 2026-09-03
대상: `시드데이터_RDS경량_파이프라인연동.md` (rev.5), `시드데이터_RDS경량_통합.sql`

---

## 1. 검토 결론

| 항목 | 결과 |
|------|------|
| MD ↔ SQL 행 수·역할 구조 | **통과** (40행 기준) |
| Admin BE Flyway 대상 기준 FK·CHECK | **조건부 통과** |
| geometry·승인 상태·교체 관계 | **수정 반영** |
| 단일 SQL 파일 | **생성 완료** |
| 로컬 DB 실행 검증 | **미실행** (파이프라인 10축제 및 Admin Flyway 선행 필요) |

---

## 2. MD에서 발견·수정한 불일치

### 2.1 역할 행 수 (44 → 40)

| 구분 | MD (수정 전) | SQL 실제 | 조치 |
|------|-------------|----------|------|
| 기본 OWNER+SUB | 30 | 30 | 유지 |
| fixture 추가 | 19행 추가 주장 | owner/SUB **치환** (행 수 불변) | MD 문구 수정 |
| A 세트 교차 | 6행으로 기술 | F2~F6 × 2 = **10행** | MD·§6 정정 |
| **최종** | 44 | **40** | §2·§4·§6·§12 반영 |

산식: `10 OWNER + 20 SUB(기본) + 10 SUB(A 교차) = 40`

### 2.2 fixture owner 대체 축제

| MD (수정 전) | SQL·§5.4 (정본) |
|--------------|------------------|
| seed_idx 1,4,5,6,7 | seed_idx **1,7,8,9,10** (F1, F7~F10) |

§6.1·§6.2를 §5.4 세트 매핑과 일치시켰다.

### 2.3 §6.3 seed_idx=5 샘플 오류

- 수정 전: `admin31`을 F5 OWNER로 표기 → **오류** (`admin31`은 F8 세트 D)
- 수정 후: bulk `owner.05`(id 5) + A 교차 `admin01`·`admin02` SUB

### 2.4 §14 분할 SQL → 통합 파일

11개 분할 파일 계획을 `시드데이터_RDS경량_통합.sql` 단일 실행으로 변경.

---

## 3. SQL에서 수정한 이슈

### 3.1 `festival_visitor_count` WHERE 우선순위

```sql
-- 수정 전 (AND가 OR 뒤에만 적용되어 seed_idx 5~6도 날짜 NULL 시 행 생성 위험)
WHERE m.seed_idx <= 4 OR (m.seed_idx BETWEEN 7 AND 8) AND ...

-- 수정 후
WHERE (m.seed_idx <= 4 OR m.seed_idx BETWEEN 7 AND 8)
  AND m.start_date IS NOT NULL AND m.end_date IS NOT NULL;
```

### 3.2 `booth_congestion` modifier CHECK

`chk_booth_congestion_modifier`: ADMIN/STAFF 중 하나만 FK 필수.

- 수정: row 1~2는 STAFF, row 3~4는 `booth_id % 3 = 0`일 때만 ADMIN
- queue가 ADMIN 수정인 부스는 `active_staff` LATERAL로 STAFF FK 보완

### 3.3 `field_staff_accounts.valid_from`

MD 명세(`start_date - 1일`)에 맞게 SQL 반영.

---

## 4. SQL 자체 검토 (정적)

### 4.1 삽입 순서·FK

| 순서 | 테이블 | FK 의존 | 판정 |
|------|--------|---------|------|
| 1 | `seed_festival_map` (TEMP) | `festivals` | OK |
| 2 | 선삭제 | 자식→부모 역순 | OK |
| 3 | `admin_accounts` | 없음 | OK |
| 4 | `admin_festival_roles` | accounts, festivals | OK |
| 5 | `field_staff_accounts` | festivals | OK |
| 6 | `festival_locations` | festivals, admin(optional) | OK |
| 7 | `festival_maps` | festivals, locations | OK |
| 8 | `festival_roadmap` | festivals, maps | OK |
| 9 | `roadmap_node` → `booth_info` → UPDATE | roadmap, maps | OK |
| 10 | `booth_queue` | booth, festival, admin/staff | OK |
| 11 | `booth_congestion` | booth, admin/staff | OK |
| 12 | 방문 인원 | festivals | OK |

### 4.2 CHECK·비즈니스 규칙

| 규칙 | SQL | 판정 |
|------|-----|------|
| `festival_locations` API→created_by NULL | `n=1` API | OK |
| `festival_locations` MANUAL→admin 필수 | `n>1` | OK |
| 축제별 primary 1개 | `is_primary = (n=1)` | OK |
| `festival_maps` current 1개/축제 | 첫 INSERT만 `is_current=true` | OK |
| `storage_status=REPLACED` | enum 존재 | OK |
| queue modifier ADMIN/STAFF XOR FK | CASE 분기 | OK |
| congestion modifier XOR FK | §3.2 수정 | OK |
| DAILY vs TOTAL 분리 | UPDATE mode + INSERT 분기 | OK |
| UNSET(5~6) 방문 0행 | INSERT 조건 제외 | OK |

### 4.3 예상 행 수 (파이프라인 10축제·날짜 정상 가정)

| 테이블 | 예상 |
|--------|-----:|
| admin_accounts | 48 |
| admin_festival_roles | 40 |
| field_staff_accounts | 20 |
| festival_locations | 25 |
| festival_maps | 13 |
| festival_roadmap | 10 |
| roadmap_node | 150 |
| booth_info | 80 |
| booth_queue | 80 |
| booth_congestion | 320 |
| festival_visitor_count | **가변** (행사일수, 약 50) |
| festival_visitor_total | 2 |

### 4.4 알려진 제한·주의

1. **고정 PK 1~48**: 비시드 계정이 예약 범위를 사용하면 사전검사에서 실패한다. 개발/시드 전용 DB 권장.
2. **재실행 범위**: 명시된 시드 namespace와 매핑 축제 범위만 삭제한다. 매핑 축제가 바뀌면 이전 축제의 장소·지도·방문 입력 잔여 여부를 별도 확인해야 한다.
3. **방문 모드 분포**: `seed_idx` 순서가 ongoing→upcoming→completed가 아니면 MD §3.3 시나리오와 어긋날 수 있음 (파이프라인 정렬 의존).
4. **S3 파일 없음**: `seed/maps/...` 키만 존재. 실제 다운로드 API는 404 가능.
5. **`map_analysis_job`**: 미포함 (MD §10.6와 동일).

---

## 5. 추가 정합성 검토 및 반영 사항

### 5.1 실행 대상 스키마

이 SQL은 `ChookJiBup_data_pipeline`에서 생성된 `festivals`를 참조하고, 나머지 관리자 테이블은 Admin BE Flyway(V1~V16)가 생성하는 것을 전제로 한다. 파이프라인 `schema.sql` 전체를 동일 DB에 적용하면 `admins`/`festival_staff`/소문자 enum 등 명칭·타입이 달라 실행 대상이 아니다.

### 5.2 geometry 검증

`roadmap_node.geometry_data`를 `MapGeometryValidator` 규칙에 맞게 수정했다. IMAGE 지도는 1.0 정규화 좌표, COORDINATE 지도는 2.0 WGS84 POINT를 사용하며, RECTANGLE·POLYGON·POLYLINE의 필수 필드를 채운다.

### 5.3 승인·교체 관계

- BOOTH 노드는 `CONFIRMED`만 생성하고 `booth_info`·queue·congestion을 연결한다.
- AI `REVIEW_REQUIRED` 노드는 시설 노드에 한정한다.
- 교체 지도는 새 current 행의 `replaces_map_id`가 이전 REPLACED 행을 가리키도록 수정했다.

### 5.4 생성 스크립트

`_generate_seed_sql.ps1`에 core 파일 존재, placeholder 제거, BEGIN/COMMIT, 필수 섹션 검사를 추가했다. core 파일은 직접 실행하지 않는다.

### 5.5 방문 모드 배정

방문 모드를 전역 `seed_idx`에만 의존하지 않도록 수정했다. `ongoing=DAILY`, `upcoming=UNSET`, `completed`는 상태별 순번(`status_rank`) 1~2가 `DAILY`, 나머지가 `TOTAL`이다. 따라서 파이프라인의 실제 상태별 축제 수가 달라도 시나리오가 다른 상태의 축제로 이동하지 않는다.

### 5.6 통합 SQL 인코딩

생성기 header를 ASCII로 변경하고 core 본문은 명시적 UTF-8로 읽고 쓰도록 유지했다. 생성 후 UTF-8 strict decode와 한글 본문 포함 여부를 확인했으며, Windows PowerShell 콘솔 코드페이지와 무관하게 파일 바이트는 정상이다.

## 6. 실행 후 검증 쿼리 (권장)

```sql
-- 행 수
SELECT 'admin_accounts' t, COUNT(*) FROM admin_accounts WHERE email LIKE '%@seed.%'
UNION ALL SELECT 'admin_festival_roles', COUNT(*) FROM admin_festival_roles afr
  JOIN festivals f ON f.festival_id = afr.festival_id
  WHERE f.festival_id IN (SELECT festival_id FROM festivals WHERE is_active LIMIT 10)
UNION ALL SELECT 'booth_congestion', COUNT(*) FROM booth_congestion;

-- 역할 40·축제당 OWNER 1
WITH seed_festival_map AS (
    SELECT ROW_NUMBER() OVER (
               ORDER BY CASE progress_status
                            WHEN 'ongoing' THEN 0
                            WHEN 'upcoming' THEN 1
                            ELSE 2
                        END,
                        start_date NULLS LAST,
                        festival_id
           ) AS seed_idx,
           festival_id
    FROM festivals
    WHERE is_active = true
      AND progress_status IN ('ongoing', 'upcoming', 'completed')
    LIMIT 10
), expected(seed_idx, expected_total) AS (
    VALUES (1, 3), (2, 5), (3, 5), (4, 5), (5, 5),
           (6, 5), (7, 3), (8, 3), (9, 3), (10, 3)
)
SELECT m.seed_idx, m.festival_id,
       COUNT(afr.id) total,
       COUNT(*) FILTER (WHERE afr.role='FESTIVAL_OWNER') owners,
       e.expected_total
FROM seed_festival_map m
JOIN expected e ON e.seed_idx = m.seed_idx
LEFT JOIN admin_festival_roles afr ON afr.festival_id = m.festival_id
GROUP BY m.seed_idx, m.festival_id, e.expected_total
HAVING COUNT(afr.id) <> e.expected_total
    OR COUNT(*) FILTER (WHERE afr.role='FESTIVAL_OWNER') <> 1;

-- geometry 기본 검증(IMAGE 지도)
SELECT COUNT(*) AS invalid_image_geometry
FROM roadmap_node rn
JOIN festival_maps fm ON fm.map_id = rn.map_id
WHERE fm.map_kind = 'IMAGE'
  AND (
      (rn.geometry_type = 'RECTANGLE'
       AND NOT (rn.geometry_data ?& ARRAY['x','y','width','height','rotation']))
      OR (rn.geometry_type IN ('POLYGON','POLYLINE')
          AND NOT (rn.geometry_data->'points' IS NOT NULL))
  );

-- 교체 방향·승인 부스 검증
SELECT COUNT(*) AS invalid_replacement_link
FROM festival_maps old_map
JOIN festival_maps current_map
  ON current_map.replaces_map_id = old_map.map_id
WHERE old_map.storage_status <> 'REPLACED'
   OR old_map.is_current;

SELECT COUNT(*) AS unapproved_booth_info
FROM booth_info bi
JOIN roadmap_node rn ON rn.id = bi.roadmap_node_id
WHERE rn.node_type = 'BOOTH' AND rn.review_status <> 'CONFIRMED';

-- modifier CHECK 위반 없음
SELECT COUNT(*) FROM booth_congestion
WHERE NOT (
  (modifier_type='ADMIN' AND modifier_admin_id IS NOT NULL AND modifier_staff_id IS NULL)
  OR (modifier_type='STAFF' AND modifier_staff_id IS NOT NULL AND modifier_admin_id IS NULL)
);

-- 방문 모드·테이블 일치
SELECT f.festival_id, f.visitor_count_input_mode,
       (SELECT COUNT(*) FROM festival_visitor_count c WHERE c.festival_id=f.festival_id) daily_rows,
       (SELECT COUNT(*) FROM festival_visitor_total t WHERE t.festival_id=f.festival_id) total_rows
FROM festivals f
WHERE f.visitor_count_input_mode IN ('DAILY','TOTAL','UNSET')
ORDER BY f.festival_id
LIMIT 20;
```

---

## 7. 산출물

| 파일 | 용도 |
|------|------|
| `시드데이터_RDS경량_통합.sql` | **실행용** 단일 스크립트 |
| `_seed_sql_core.sql` | 코어 로직 (재생성 소스) |
| `_generate_seed_sql.ps1` | 통합 파일 재생성 |
| `시드데이터_RDS경량_파이프라인연동.md` | rev.5 명세 (본 검토 반영) |
