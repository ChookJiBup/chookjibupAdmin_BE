문서 형식: 운영 명세

# 시드 데이터 (RDS 경량) — 파이프라인 축제 연동

작성일: 2026-09-03 (rev.5)
대상: `chookjibupAdmin_BE` Flyway/JPA 스키마 + 공유 DB (`ChookJiBup_data_pipeline`가 적재한 `festivals`)
**축제 마스터는 시드하지 않음** — 파이프라인 `festivals.festival_id` FK 참조

> 실행 계약: 파이프라인은 `festivals`와 파이프라인 전용 테이블만 소유하고, `admin_accounts`·`field_staff_accounts`·지도·부스·방문 입력 테이블은 Admin BE Flyway(V1~V16)가 소유한다. `ChookJiBup_data_pipeline/schema.sql`의 `admins`·`festival_staff` 등 별도 명칭 테이블을 같은 대상에 함께 적용하지 않는다.

---

## 1. 비밀번호 (BCrypt — Spring Security 동일)

| 항목 | 값 |
|------|-----|
| 평문 (로그인·스태프) | `qwer1234` |
| `password_hash` (DB 저장값) | `$2a$10$SquZ7eQJgGuMtAB3lvtureY0RtvrWorpA4ENzrRjqhAb7ONCBWffy` |

- Admin BE `BCryptPasswordEncoder`(strength 10)로 생성·검증됨.
- 관리자·스태프 **전 계정 동일 해시** 사용 (시드 SQL에 위 문자열 literal INSERT).
- 재생성: 로컬에서 `BCryptPasswordEncoder().encode("qwer1234")` 1회 실행 후 치환.

---

## 2. 규모 요약 (RDS 경량 프로파일)

| 구분 | 이전 대량안(참고) | **현재(경량)** | 시드 |
|------|--------:|---------------:|:----:|
| 관리자 계정(대량 기본) | 170 | **30** | O |
| 연결 테스트 fixture | - | **18 (6세트)** | O※ |
| 축제 역할(대량 기본) | 170 | **30** | O |
| 축제 역할(A 세트 교차 추가) | - | **10** | O※ |
| **축제 역할 최종** | - | **40** | O |
| 현장 스태프 | 180 | **20** | O |
| 축제 장소 | - | **25** | O※ |
| 지도(현재+교체 이력) | - | **13** | O※ |
| 축제 배치도 | - | **10** | O※ |
| 배치도 노드(시설 포함) | - | **150** | O※ |
| 승인 부스 | - | **80** | O※ |
| 대기열 | 240 | **80** | O※ |
| 혼잡 이력 | 480 | **320** | O※ |
| 일자별 방문 인원 행 | - | **약 50** | O※ |
| 총 방문 인원 행 | - | **2** | O※ |

※ `booth_queue`·`booth_congestion`은 먼저 동일 축제의 `booth_info`가 필요하다. 지도·노드·부스·방문 인원은 파이프라인 축제 ID를 매핑한 뒤 보완 시드로 적재한다.
※ 연결 테스트 fixture 18개는 대량 관리자 30명과 별도이며, 세트별 연결 역할은 아래 5.4에 정의한다.

### 2.1 확장 데이터의 의도

- 장소는 축제당 1~5개로 분산한다. (3·3·2·2개 축제에 1·2·3·5개 = 총 25개)
- 지도는 10개 현재본과 3개 교체 이력을 넣어 S3 메타데이터, 현재본 유일성, 교체 조회를 검증한다.
- 노드는 부스뿐 아니라 무대·화장실·통로·출입구·안내소를 포함한다. BOOTH 80개는 모두 `CONFIRMED` 상태로 만들고, 시설 노드만 AI 검수 대상을 포함한다.
- 방문 인원은 `DAILY`(6축제)와 `TOTAL`(2축제)을 분리하고, `UNSET` 축제에는 방문 행을 넣지 않는다.
- 대기열 80개는 0m·경계값(10/11/30/31m)·장거리 값을 포함하며 부스당 혼잡 이력 4개를 생성한다.

**파이프라인 축제 사용 수**: **10개** (`seed_idx` 1..10)

### 2.2 RDS 적용 원칙

- 기본 실행은 본 경량 프로파일만 사용하고, 대량(100축제 이상) 시드는 별도 스크립트로 분리한다.
- `map_analysis_job` 13행은 기본 시드에서 생략한다. AI Worker를 실제로 켤 때만 별도 스크립트를 추가한다.
- 이미지 파일을 RDS에 저장하지 않고 S3 더미 object key·메타데이터만 저장해 디스크와 백업 용량을 줄인다.
- **단일 SQL 파일**(`시드데이터_RDS경량_통합.sql`)을 `BEGIN`/`COMMIT` 한 트랜잭션으로 실행한다. FK 검증 실패 시 전체 롤백한다. 운영 데이터가 있는 RDS에서는 `TRUNCATE ... CASCADE`를 사용하지 않는다.
- `_seed_sql_core.sql`은 placeholder가 남은 생성 소스이므로 직접 실행하지 않고 `_generate_seed_sql.ps1` 결과만 실행한다.
- 고정 관리자 ID 1~48을 사용하므로 실행 전 비시드 계정의 예약 범위 충돌 검사를 통과해야 한다.

---

## 3. 파이프라인 선행

```bash
cd C:\projects\chookjibup\ChookJiBup_data_pipeline
python main.py
```

### 3.1 축제 10개 선정

```sql
CREATE TEMP TABLE seed_festival_map AS
SELECT
    ROW_NUMBER() OVER (
        ORDER BY
            CASE progress_status WHEN 'ongoing' THEN 0 WHEN 'upcoming' THEN 1 ELSE 2 END,
            start_date NULLS LAST,
            festival_id
    ) AS seed_idx,
    ROW_NUMBER() OVER (
        PARTITION BY progress_status
        ORDER BY start_date NULLS LAST, festival_id
    ) AS status_rank,
    festival_id,
    public_id,
    festival_name,
    progress_status,
    start_date,
    end_date
FROM festivals
WHERE is_active = true
  AND progress_status IN ('ongoing', 'upcoming', 'completed')
LIMIT 10;
```

| seed_idx | festival_id | festival_name | progress_status |
|---------:|------------:|---------------|-----------------|
| 1 | `{PIPE.F1}` | (조회값) | ongoing |
| 2 | `{PIPE.F2}` | … | ongoing |
| … | … | … | … |
| 10 | `{PIPE.F10}` | … | … |

### 3.2 파이프라인 소유 데이터와 관리자 보완 데이터

| 구분 | 테이블 | 처리 원칙 |
|------|--------|-----------|
| 파이프라인 소유 | `festival_series`, `festivals`, `festival_visitor_excel`, `festival_visitor_yearly` | 파이프라인 실행 결과를 사용하고 Admin 시드에서 INSERT하지 않는다. |
| 관리자 보완 | `festival_locations`, `festival_maps`, `festival_roadmap`, `roadmap_node`, `booth_info` | `seed_festival_map`의 `festival_id`만 FK로 참조해 생성한다. |
| 관리자 운영 | `booth_queue`, `booth_congestion`, 방문 입력 테이블 | 보완 데이터가 성공한 뒤 동일 축제·부스 관계를 검증하고 생성한다. |

모든 보완 행은 `festival_id`를 숫자 순번으로 가정하지 않는다. 실행 시 생성된 `seed_festival_map`을 조인하고, 재실행 시 `seed_batch_id`(실제 컬럼을 추가하지 않는다면 임시 매핑/전용 시드 스키마)를 기준으로 기존 행을 정리해 중복을 방지한다.

### 3.3 축제 상태·방문 모드 분포

| 축제 묶음 | 수량 | 방문 모드 | 추가 조건 |
|-----------|----:|-----------|-----------|
| ongoing | 4 | `DAILY` | 시작일 이후 오늘까지 일부 일자만 입력 |
| upcoming | 2 | `UNSET` | 방문 데이터 없음, 장소·지도만 존재 |
| completed | 4 | `DAILY` 2 / `TOTAL` 2 | 일자별 누적 또는 총원 중 하나만 입력 |

파이프라인의 실제 축제 수가 위 분포와 다르더라도 수량을 억지로 맞추지 않는다. SQL은 `progress_status`를 우선 사용하고, completed 축제는 `status_rank` 1~2를 `DAILY`, 나머지를 `TOTAL`로 배정한다. ongoing은 `DAILY`, upcoming은 `UNSET`으로 처리하며 일자 데이터는 실제 날짜 범위 안에서만 생성한다.

---

## 4. 삽입 순서

```text
[파이프라인] festivals
    ↓
admin_accounts (30 + fixture 18)
    ↓
admin_festival_roles (기본 30 + A 교차 10 = **40행**)
    ↓
field_staff_accounts (20)
    ↓
festival_locations (25)
    ↓
festival_maps (13) → festival_roadmap (10)
    ↓
roadmap_node (150) → booth_info (80)
    ↓
booth_queue (80) + booth_congestion (320)
    ↓
festival_visitor_count / festival_visitor_total
```

`roadmap_node.related_booth_id`와 `booth_info.roadmap_node_id`가 서로 FK인 경우에는 노드를 먼저 NULL로 만들고, 부스 생성 후 노드의 연결 컬럼을 UPDATE한다. 지도·로드맵의 현재본 FK가 생성된 뒤에만 운영 데이터를 넣는다.

---

## 5. `admin_accounts` — 30명 + fixture 18명

| id | account_kind | 역할 | email 패턴 | organization |
|---:|--------------|------|------------|--------------|
| 1–10 | GOVERNMENT | 총괄(축제 10) | `owner.{nn}@seed.mapo.go.kr` | 과·팀 순환 |
| 11–20 | GOVERNMENT | 운영자 10명 | `op.{nn}@seed.mapo.go.kr` | 3과 순환 |
| 21–30 | CONTRACTOR | 외부 10명 | `contractor.{nn}@seed-event.co.kr` | 업체명 |

공통 컬럼:

```text
password_hash = '$2a$10$SquZ7eQJgGuMtAB3lvtureY0RtvrWorpA4ENzrRjqhAb7ONCBWffy'
auth_version  = 0
status        = 'ACTIVE'
public_id     = uuid (고정 패턴 또는 gen_random_uuid())
```

### 5.1 생성 공식

```text
id 1..10 (총괄):
  email = owner.{id:02d}@seed.mapo.go.kr
  name  = {김,이,박,최,정,강,조,윤,장,임}[id%10]총괄{id:02d}
  org   = [관광정책과, 문화관광과, 축제추진단, 도시디자인과, 안전총괄과][id%5]
  rank  = 과장 (id%7==0 → 단장)

id 11..20 (운영 10):
  n = id - 10
  email = op.{n:02d}@seed.mapo.go.kr
  name  = {성씨}운영{n:02d}
  org   = [관광정책과, 도시디자인과, 안전총괄과][n%3]
  rank  = [주무관, 대리, 사무관][n%3]

id 21..30 (외부 10):
  n = id - 20
  email = contractor.{n:02d}@seed-event.co.kr
  name  = 외부운영{n:02d}
  org   = 이벤트업체{n:02d}
  rank  = NULL
```

### 5.2 샘플 10건

`owner.01` 계정은 대량 계정 샘플로 남기되, F1의 `FESTIVAL_OWNER` 역할은 연결 테스트 fixture `admin01`이 대신 가진다.

| id | email | name | kind |
|---:|-------|------|------|
| 1 | owner.01@seed.mapo.go.kr | 김총괄01 | GOVERNMENT |
| 2 | owner.02@seed.mapo.go.kr | 이총괄02 | GOVERNMENT |
| 10 | owner.10@seed.mapo.go.kr | 임총괄10 | GOVERNMENT |
| 11 | op.01@seed.mapo.go.kr | 김운영01 | GOVERNMENT |
| 15 | op.05@seed.mapo.go.kr | 장운영05 | GOVERNMENT |
| 20 | op.10@seed.mapo.go.kr | 임운영10 | GOVERNMENT |
| 21 | contractor.01@seed-event.co.kr | 외부운영01 | CONTRACTOR |
| 25 | contractor.05@seed-event.co.kr | 외부운영05 | CONTRACTOR |
| 30 | contractor.10@seed-event.co.kr | 외부운영10 | CONTRACTOR |

### 5.3 대표 개발용 관리자 계정

별도의 전역 `SUPER_ADMIN` 역할은 현재 Admin BE에 정의되어 있지 않다. 따라서 메인 로그인 계정은 연결 테스트용 고정 fixture인 `admin01`로 지정한다.

| 구분 | 계정 | 비밀번호 | 권한 범위 |
|------|------|----------|-----------|
| 대표 개발 계정 | `admin01@seed.mapo.go.kr` | `qwer1234` | `{PIPE.F1}`의 `FESTIVAL_OWNER` |

이 계정은 모든 축제를 무제한 관리하는 전역 관리자가 아니다. 여러 축제를 한 계정으로 점검해야 하면 별도 `SUB_ADMIN` 배정 행을 추가하되, 축제별 `FESTIVAL_OWNER`는 기존 총괄 계정 한 명을 유지한다.

### 5.4 관리자 연결 테스트 6세트

대량 계정(30명)과 별도로 아래 18개 계정을 **고정 fixture**로 만든다. 이메일로 조회하므로 DB에서 생성된 실제 PK는 실행 시 확정한다. A 세트는 여러 축제에 교차 배정해 거미줄형 권한 그래프를 만들고, B 세트는 기존처럼 미연결 상태를 유지한다.

| 세트 | 계정 3개 | 기본 연결 | 연결 정책 |
|------|----------|-----------|-----------|
| A | `admin01`(제1) · `admin02`(제2) · `admin03`(외부) | `{PIPE.F1}` | F1~F6에 교차 SUB_ADMIN 배정(아래 매트릭스) |
| B | `admin11`(제1 후보) · `admin12`(제2 후보) · `admin13`(외부 후보) | 없음 | 사용자가 초대·수락으로 직접 연결 |
| C | `admin21` · `admin22` · `admin23` | `{PIPE.F7}` | 제1·제2·외부를 한 축제에 단순 연결 |
| D | `admin31` · `admin32` · `admin33` | `{PIPE.F8}` | 제1·제2·외부를 한 축제에 단순 연결 |
| E | `admin41` · `admin42` · `admin43` | `{PIPE.F9}` | 제1·제2·외부를 한 축제에 단순 연결 |
| F | `admin51` · `admin52` · `admin53` | `{PIPE.F10}` | 제1·제2·외부를 한 축제에 단순 연결 |

각 세트의 번호가 낮은 계정(`admin01`, `admin11`, `admin21` 등)은 GOVERNMENT 제1 관리자 후보, 가운데 계정은 GOVERNMENT 제2 관리자 후보, 끝 계정(`admin03`, `admin13`, `admin23` 등)은 CONTRACTOR 외부 관리자 후보로 생성한다.

#### A 세트 거미줄형 연결 매트릭스

각 축제의 기존 대량 시드 총괄은 `FESTIVAL_OWNER`로 유지하고, A 세트는 `SUB_ADMIN`으로 교차 배정한다. 단, F1의 기존 `owner.01` 역할만 `admin01` fixture로 대체한다.

| 축제 | FESTIVAL_OWNER | 추가 SUB_ADMIN |
|------|----------------|----------------|
| F1 | `admin01` | `admin02`, `admin03` |
| F2 | 기존 owner | `admin01`, `admin02` |
| F3 | 기존 owner | `admin02`, `admin03` |
| F4 | 기존 owner | `admin01`, `admin03` |
| F5 | 기존 owner | `admin01`, `admin02` |
| F6 | 기존 owner | `admin02`, `admin03` |

이렇게 하면 세 계정이 각각 여러 축제에 연결되고, F1~F6도 둘 이상의 A 계정과 연결된다. 기존 B 세트(`admin11`~`admin13`)에는 역할 행을 만들지 않는다.

모든 fixture의 비밀번호는 `qwer1234`로 통일한다. C~F 세트의 외부 계정도 연결 후 `SUB_ADMIN`만 허용한다. B 세트를 연결할 때는 실제 초대·수락 API를 사용해 역할 행이 생성되는지 확인한다.

권장 테스트 순서는 `admin11` 로그인 → F1 초대 후보 조회 → `admin01`로 초대 발송 → `admin11` 수락 → F1 역할·기능 확인이다. 이어서 A 세트로 다중 축제 접근, C~F 세트로 단일 축제 접근, 외부 계정의 `FESTIVAL_OWNER` 승격 거부를 확인한다.

---

## 6. `admin_festival_roles` — 기본 30 + A 교차 10 (최종 **40행**)

축제 `seed_idx` (1..10)마다:

| # | admin_id | role | invited_by |
|---|--------:|------|----------:|
| 1 | `seed_idx` (일부 축제는 fixture로 대체) | FESTIVAL_OWNER | NULL |
| 2 | `11 + (seed_idx-1)` | SUB_ADMIN | owner |
| 3 | `21 + (seed_idx-1)` | SUB_ADMIN | owner |

> 공무 운영 1명 + 외부 1명 = 10×2 SUB + 10 OWNER = **30**. A 세트 교차 SUB 10행을 F2~F6에 추가해 **최종 40행**이다. fixture owner·SUB 대체는 기본 30행 안에서 치환되며 행 수를 늘리지 않는다.

### 6.1 공식 (기본 30)

```text
seed_idx = 1 .. 10
festival_id = seed_festival_map[seed_idx]
owner_id = seed_idx (단, seed_idx=1,7,8,9,10은 fixture PK로 대체)

roles:
  (owner_id, FESTIVAL_OWNER, NULL)
  (11 + seed_idx - 1, SUB_ADMIN, owner_id)
  (21 + seed_idx - 1, SUB_ADMIN, owner_id)
```

검증: 대량 계정 범위 1..30 안에서만 FK를 참조한다. fixture로 대체한 owner 행은 중복 INSERT하지 않는다.

### 6.2 샘플 — seed_idx=1, festival=`{PIPE.F1}`

아래는 기본 역할 중 F1 owner 대체와 A fixture 추가분만 표시한 것이다(대량 SUB_ADMIN 2행은 별도 존재).

| role_row | festival_id | admin_id | role | invited_by |
|---------:|------------:|---------:|------|----------:|
| 1 | {PIPE.F1} | `admin01` | FESTIVAL_OWNER | NULL |
| 2 | {PIPE.F1} | `admin02` | SUB_ADMIN | `admin01` |
| 3 | {PIPE.F1} | `admin03` | SUB_ADMIN | `admin01` |

기본 30개 역할 슬롯 중 F1·F7~F10의 owner 5행은 fixture 계정으로 대체한다. F1의 SUB 2행도 `admin02`·`admin03`으로 대체한다. 여기에 A 세트 교차 SUB_ADMIN 10행(F2~F6)을 추가한다. **최종 역할 행 수는 40행**이다.

### 6.3 샘플 — seed_idx=5 (F5, 대량 계정 유지)

| admin_id | role | email |
|---------:|------|-------|
| 5 | FESTIVAL_OWNER | owner.05@seed.mapo.go.kr |
| 15 | SUB_ADMIN | op.05@… |
| 25 | SUB_ADMIN (외부) | contractor.05@… |
| 31, 32 | SUB_ADMIN (A 세트 교차) | admin01@…, admin02@… |

---

## 7. `field_staff_accounts` — 20 (= 10축제 × 2)

```text
staff_id = 1 .. 20
seed_idx = (staff_id - 1) // 2 + 1         -- 1..10
seq      = (staff_id - 1) % 2 + 1         -- 1..2
festival_id = seed_festival_map[seed_idx].festival_id

login_id    = staff-{seed_idx:02d}-{seq:02d}
name        = 스태프{seed_idx:02d}-{seq:02d}
phone       = 010-40{staff_id:03d}-{staff_id:04d}
password_hash = (동일 BCrypt)
valid_from  = festivals.start_date - interval '1 day'
valid_until = festivals.end_date + interval '1 day'
status      = ACTIVE (staff_id % 7 == 0 → INACTIVE, staff_id % 11 == 0 → DELETED 제외)
```

### 7.1 샘플 — `{PIPE.F1}` 2명 (staff_id 1–2)

| id | login_id | name | phone | status |
|---:|----------|------|-------|--------|
| 1 | staff-01-01 | 스태프01-01 | 010-40001-0001 | ACTIVE |
| 2 | staff-01-02 | 스태프01-02 | 010-40002-0002 | ACTIVE |

### 7.2 샘플 — `{PIPE.F10}` (staff_id 19–20)

| id | login_id |
|---:|----------|
| 19 | staff-10-01 |
| 20 | staff-10-02 |

### 7.3 비활성·삭제 계정 경계 케이스

`staff_id % 7 == 0`은 `INACTIVE`, `staff_id % 11 == 0`은 `DELETED`로 설정한다. 유효 기간은 행사 종료+1일까지 유지하고, completed 축제의 종료일이 지난 스태프는 기간 만료 로그인 실패 케이스로 사용한다.

---

## 8. `booth_queue` — 80

```sql
SELECT booth_id, festival_id, booth_name
FROM booth_info
ORDER BY festival_id, booth_id
LIMIT 80;
```

```text
queue_id = 1 .. 80
booth_id = B[queue_id]
queue_tail_meters = [0,3,5,8,10,12,15,18,20,22,25,28,30,35,40,45,50,55,60][queue_id % 19]
modifier: queue_id % 7 == 0 → ADMIN (해당 festival owner_id)
          else → STAFF (staff_id = (seed_idx-1)*2 + (queue_id % 2) + 1)
```

### 8.1 혼잡 추정 (줄끝 PATCH 연동)

| queue_tail_meters | wait_minutes | congestion_level |
|------------------:|-------------:|------------------|
| 0–10 | ceil(m/10)×10 | LOW |
| 11–30 | … | MEDIUM |
| 31+ | … | HIGH |

### 8.2 샘플 20건

| q_id | booth_id | festival | meters | wait | level | modifier |
|-----:|---------:|----------|-------:|-----:|-------|----------|
| 1 | B1 | F1 | 8 | 10 | LOW | STAFF 1 |
| 2 | B2 | F1 | 25 | 30 | MEDIUM | STAFF 2 |
| 3 | B3 | F1 | 35 | 40 | HIGH | STAFF 1 |
| 4 | B4 | F1 | 12 | 20 | MEDIUM | STAFF 4 |
| 5 | B5 | F1 | 0 | 0 | LOW | STAFF 5 |
| 6 | B6 | F1 | 42 | 50 | HIGH | STAFF 2 |
| 7 | B7 | F1 | 55 | 60 | HIGH | ADMIN 1 |
| 8 | B8 | F1 | 18 | 20 | MEDIUM | STAFF 1 |
| 9 | B9 | F2 | 5 | 10 | LOW | STAFF 3 |
| 10 | B10 | F2 | 30 | 30 | MEDIUM | STAFF 4 |
| 11 | B11 | F2 | 22 | 30 | MEDIUM | STAFF 3 |
| 12 | B12 | F2 | 15 | 20 | MEDIUM | STAFF 4 |
| … | … | … | … | … | … | … |
| 80 | B80 | … | 48 | 50 | HIGH | STAFF … |

---

## 9. `booth_congestion` — 320 (= 80부스 × 4이력)

부스당 4행 (과거 → 중간 → 최신 직전 → 최신):

```text
for booth_idx 1..80:
  congestion_id = (booth_idx-1)*4 + row (1..4)
  booth_id = B[booth_idx]
  row1: -180min, row2: -90min, row3: -30min, row4: now
  wait/level: queue_tail 기준 -1/동일/+1 step 변화
  modifier: row1 STAFF, row2 STAFF, row3 ADMIN, row4 ADMIN or STAFF (booth_idx%3)
```

| booth | 이력1 (3h 전) | 이력2 (90m 전) | 이력3 (30m 전) | 이력4 (최신) |
|-------|---------------|----------------|----------------|--------------|
| B1 | MEDIUM 20 | LOW 10 | LOW 10 | LOW 10 |
| B2 | MEDIUM 30 | MEDIUM 30 | HIGH 40 | HIGH 40 |
| B3 | HIGH 50 | HIGH 40 | HIGH 40 | MEDIUM 30 |

---

## 10. 축제 장소·지도·배치도 보완 시드

### 10.1 `festival_locations` — 25

축제별 장소 수를 `seed_idx` 구간으로 분배한다. 1~3번은 1개, 4~6번은 2개, 7~8번은 3개, 9~10번은 5개를 만들어 총 25행을 구성한다. 축제마다 `is_primary = true`는 정확히 1행이어야 한다.

```text
location_type = MAIN_VENUE(기본), SUB_VENUE/STAGE_AREA/EXPERIENCE_AREA/PARKING/ENTRANCE(추가)
location_name = {축제명} 주행사장 / 부행사장-{n}
source_type   = API(파이프라인 주소), MANUAL(관리자 추가 주소)
API 행        = created_by_admin_id NULL, 도로명·좌표 중 하나 이상
MANUAL 행     = created_by_admin_id 유효 관리자 ID, 좌표·주소 중 하나 이상
sort_order    = 0부터 연속
```

기본 장소는 `festival.road_address`를 우선 사용하고, 추가 장소는 서로 다른 좌표(반경 100m 이상)를 사용한다. 좌표는 위도 `-90..90`, 경도 `-180..180` 범위를 지키며 위도·경도 중 하나만 채운 행은 만들지 않는다.

| 필드 | 생성 규칙 예시 |
|------|----------------|
| `public_id` | 행마다 UUID v4, 재실행 시 동일 축제의 기존 UUID 재사용 금지 |
| `road_address` / `jibun_address` | 파이프라인 주소 복사 또는 `({축제명}) 부행사장-{n}` 더미 주소 |
| `detail_address` / `postal_code` | 30% NULL, 나머지는 행사장·동/층 정보 |
| `latitude` / `longitude` | 기준 좌표 + `seed_idx·0.001` 오프셋 |
| `boundary_geometry` | 20%만 GeoJSON Polygon, 나머지는 NULL |

### 10.2 `festival_maps` — 13

- 10개 축제에 현재 지도 1개씩(`is_current = true`, `map_kind = COORDINATE` 또는 `IMAGE`).
- 3개 축제에는 이전 지도 1개를 추가해 교체 이력을 만든다(`is_current = false`, 새 current 지도의 `replaces_map_id`가 이전 지도 ID를 가리킨다).
- `IMAGE` 지도는 원본·표시·분석 object key, content type, file size, width/height, SHA-256을 모두 채운다. 실제 S3 업로드는 하지 않고 `seed/...` 더미 키를 사용한다.
- `COORDINATE` 지도도 현재 엔티티의 이미지 메타데이터 NOT NULL 제약을 따른다. 실제 파일 대신 `seed/maps/{festival_id}/...` 더미 키, `image/png`, 파일 크기·차원·SHA-256 결정값을 채우고 지도명·장소 ID·생성 관리자·버전을 필수로 둔다.

### 10.3 `festival_roadmap` — 10

축제당 하나만 생성하며 `current_map_id`는 해당 축제의 현재 지도만 참조한다. `zones`에는 최소 3개(메인·푸드·안전)와 최대 8개의 JSON 영역을 넣고, `edit_revision`·`published_version` 조합을 `0/0`, `2/1`, `5/3`으로 분산한다. 상태는 `ANALYZING` 10%, `REVIEW_REQUIRED` 20%, `EDITING` 50%, `PUBLISHED` 20%로 구성한다. 기본 시드에는 `map_analysis_job`을 만들지 않으므로 `ANALYZING` 행은 Worker를 실행하지 않는 환경에서 `REVIEW_REQUIRED`로 바꾸거나, 별도 job 시드를 함께 적용해야 한다.

### 10.4 `roadmap_node` — 150 / `booth_info` — 80

축제당 노드 15개(부스 8 + 무대·화장실·통로·출입구·안내소 등 시설 7)를 만든다.

| 노드 유형 | 수량(전체) | `review_status` | `geometry_type` |
|-----------|-----------:|----------------|----------------|
| BOOTH | 80 | CONFIRMED 100% (일부는 AI 인식 후 관리자 확정) | RECTANGLE/POLYGON |
| STAGE/RESTROOM/PATH/ENTRY/INFO | 70 | CONFIRMED 70%, REVIEW_REQUIRED 30% | RECTANGLE/POINT/POLYGON/POLYLINE |

AI 분석 노드는 시설 노드에 한해 `source = AI`, `confidence`를 0.55~0.99로 넣고, 관리자 확정 노드는 `source = ADMIN`, `review_status = CONFIRMED`로 넣는다. `booth_info`는 `review_status = CONFIRMED`인 BOOTH 노드 80개에만 연결한다. 양방향 FK가 있으므로 다음 3단계로 처리한다.

```text
1) roadmap_node 생성(related_booth_id = NULL)
2) booth_info 생성(roadmap_node_id = node.id)
3) 해당 node.related_booth_id = booth.booth_id UPDATE
```

노드의 `geometry_data`는 지도 종류에 따라 검증 가능한 JSON으로 만든다. `IMAGE` 지도(`schema 1.0`)는 POINT의 `x/y`, RECTANGLE의 `x/y/width/height/rotation`, POLYGON·POLYLINE의 `points` 배열을 사용하고 모든 정규화 좌표를 0~1 범위로 제한한다. `COORDINATE` 지도(`schema 2.0`)는 WGS84 `lat/lng` POINT를 사용한다. `sort_order`는 로드맵 내에서 유일하게 유지한다.

#### 대표 픽스처 샘플

| 축제 | 장소 | 지도/로드맵 | 노드 예시 | 부스 연결 |
|------|------|-------------|-----------|-----------|
| F1 | 주행사장 1개 | COORDINATE, ANALYZING | 무대·화장실·출입구 | 부스 8개 |
| F4 | 주행사장 + 부행사장 | IMAGE, EDITING | AI 시설 노드(confidence 0.61) | 확정 부스 8개 |
| F7 | 3개 행사 구역 | IMAGE 교체 완료 | 통로 POLYLINE·안내소 POINT | 확정 부스 8개 |
| F9 | 5개 다지역 | 현재 지도 + 이전 지도 | 주차장·셔틀·출구 | 확정 부스 8개 |
| F10 | 5개 다지역 | 분석 실패 후 재시도 | `REVIEW_REQUIRED` 노드 혼합 | 확정 부스 8개 |

### 10.5 방문 인원 — 일자별 약 50 / 총원 2

`festival.visitor_count_input_mode`와 저장 테이블을 일치시킨다.

| 모드 | 대상 | 저장 규칙 |
|------|------|-----------|
| `DAILY` | ongoing 4 + completed 2 | `festival_visitor_count`에 행사 기간 내 날짜만 저장. 행사 중인 축제는 오늘 이후 날짜를 만들지 않는다. |
| `TOTAL` | completed 2 | `festival_visitor_total`에 축제당 정확히 1행 저장. |
| `UNSET` | upcoming 2 | 두 방문 테이블 모두 0행. |

일자별 값은 0, 평일 100~999, 주말 1,000~9,999를 섞고 `(festival_id, visit_date)` 유일성을 지킨다. `TOTAL` 축제에는 일자별 행을 만들지 않으며, 한 축제에 두 모드를 동시에 저장하지 않는다.

| 축제 | 입력 모드 | 예시 데이터 | 검증 포인트 |
|------|-----------|-------------|-------------|
| F1 (ongoing) | DAILY | 8/29=0, 8/30=1,240 | 0명 허용, 오늘 이후 금지 |
| F3 (ongoing) | DAILY | 행사 1일차만 530 | 부분 입력 허용 |
| F7 (completed) | DAILY | 행사 기간 5일 모두 입력 | 합계 = 일자별 합 |
| F8 (completed) | TOTAL | `total_visitor_count=18,500` | 일자별 행 0개 |
| F5 (upcoming) | UNSET | 저장 행 없음 | 조회 응답 null/0 정책 확인 |

### 10.6 선택: `map_analysis_job` 분석 상태 픽스처

지도 파이프라인 검증이 필요할 때만 13행을 추가한다. 상태를 `PENDING` 2, `PROCESSING` 1, `COMPLETED` 8, `FAILED` 2로 만들고, 실패 행에는 `error_code`·`error_message`를 넣는다. `COMPLETED` 행만 `roadmap_node.analysis_job_id`를 참조한다.

### 10.7 대량 생성 공통 규칙

반복 INSERT를 수작업으로 나열하지 않고 `seed_festival_map`과 `generate_series`를 사용하는 set-based SQL로 만든다. 아래는 각 보완 파일에서 공유하는 형태다.

```sql
-- 장소 수: 3/3/2/2 축제에 1/2/3/5개
WITH festival_slots AS (
    SELECT m.festival_id, m.seed_idx,
           CASE WHEN m.seed_idx <= 3 THEN 1
                WHEN m.seed_idx <= 6 THEN 2
                WHEN m.seed_idx <= 8 THEN 3 ELSE 5 END AS location_count
    FROM seed_festival_map m
)
SELECT f.festival_id, s.n
FROM festival_slots f
CROSS JOIN LATERAL generate_series(1, f.location_count) s(n);
```

```sql
-- DAILY 방문 행은 실제 행사 기간과 오늘 사이의 교집합만 생성
SELECT m.festival_id, d::date AS visit_date,
       CASE WHEN extract(isodow FROM d) IN (6, 7)
            THEN 1000 + ((m.seed_idx * 131 + d::date - m.start_date) % 9000)
            ELSE (m.seed_idx * 37 + d::date - m.start_date) % 1000 END AS visitor_count
FROM seed_festival_map m
CROSS JOIN LATERAL generate_series(
    m.start_date,
    LEAST(m.end_date, CURRENT_DATE),
    interval '1 day'
) d
WHERE m.progress_status IN ('ongoing', 'completed')
  AND m.start_date IS NOT NULL
  AND m.end_date IS NOT NULL;
```

생성 파일은 공통적으로 `BEGIN`/`COMMIT`, FK 선검증, 예약 PK 충돌 검사, 재실행 시 정리 범위를 포함한다. 운영 데이터가 존재하는 공유 DB에서는 전체 테이블을 `TRUNCATE`하지 않고, 매핑된 축제 ID와 명시된 시드 namespace(`owner.*`, `op.*`, `contractor.*`, `admin*@seed.*`)로 한정 삭제한다.

---

## 11. 파이프라인에서 시드 제외하는 테이블

`festival_series`, `festivals`, `festival_visitor_yearly`, `festival_visitor_excel`,
그리고 파이프라인 전용 원천·집계 테이블은 Admin 시드에서 INSERT하지 않는다. Admin DB의
`festival_locations`, `festival_maps`, `festival_roadmap`, `roadmap_node`, `booth_info`,
`festival_visitor_count`, `festival_visitor_total`은 본 문서의 보완 시드 대상이다.

---

## 12. 정합성 체크

| # | 규칙 |
|---|------|
| C1 | 대량 30 + fixture 18개, email 총 48 유일 |
| C2 | 기본 역할 30 + A 교차 10 = **40행**, `(admin_account_id, festival_id)` 유일 |
| C3 | festival_id 10개 FK 유효 |
| C4 | 대량 30 + fixture 18개 password_hash 전원 동일 BCrypt |
| C5 | `(festival_id, login_id)` 20 유일 |
| C6 | `festival_locations` 25, 축제별 primary 정확히 1개 |
| C7 | `festival_maps` 13, 축제별 current 지도 최대 1개, 교체 이력 FK 유효 |
| C8 | `festival_roadmap` 10, `current_map_id`가 동일 축제 지도 참조 |
| C9 | `roadmap_node` 150, `booth_info` 80, 노드↔부스 양방향 연결 일치 |
| C10 | `booth_queue` 80, `booth_congestion` 320, queue·booth의 festival_id 일치 |
| C11 | 방문 `DAILY`는 일자별 테이블만, `TOTAL`은 총원 테이블만, `UNSET`은 양쪽 0행 |
| C12 | 혼잡 modifier가 ADMIN이면 관리자 FK, STAFF이면 스태프 FK만 채움 |
| C13 | CONTRACTOR → `SUB_ADMIN` only, 비활성·삭제·기간 만료 스태프는 로그인 실패 |
| C14 | 동일 시드 매핑으로 재실행 시 행 수가 증가하지 않고 기존 시드만 교체 |
| C15 | 관리자 ID 1~48이 비시드 계정과 충돌하지 않음(충돌 시 전체 롤백) |
| C16 | geometry schema별 필수 필드·좌표 범위가 `MapGeometryValidator`를 통과 |

### 12.1 경계·실패 시나리오

| 영역 | 반드시 포함할 케이스 |
|------|----------------------|
| 장소 | 단일 장소, 2개 분산 장소, 3개 복수 장소, 5개 다지역 장소; 좌표만·도로명만·경계 JSON |
| 지도 | COORDINATE 지도, IMAGE 지도, 현재 지도 교체, 삭제(soft delete) 지도 |
| AI 배치도 | `REVIEW_REQUIRED` 노드, 낮은 confidence, 분석 실패 job, 관리자 확정 노드 |
| 방문 인원 | 기간 1일, 0명 일자, 오늘 이후 일자 금지, DAILY/TOTAL 혼용 금지 |
| 대기열 | 0/10/11/30/31m 경계, `path_geometry` NULL·유효 JSON, ADMIN·STAFF 수정 |
| 권한 | 다른 축제 부스 조회·수정 거부, 외부 계약자 축제 범위 밖 접근 거부 |

---

## 13. 로그인 테스트

| 용도 | 계정 | 비밀번호 |
|------|------|----------|
| 연결 완료 세트 A — 제1 관리자 | `admin01@seed.mapo.go.kr` | qwer1234 |
| 연결 완료 세트 A — 제2 관리자 | `admin02@seed.mapo.go.kr` | qwer1234 |
| 연결 완료 세트 A — 외부 관리자 | `admin03@seed-event.co.kr` | qwer1234 |
| 연결 테스트 세트 B — 제1 관리자 후보 | `admin11@seed.mapo.go.kr` | qwer1234 |
| 연결 테스트 세트 B — 제2 관리자 후보 | `admin12@seed.mapo.go.kr` | qwer1234 |
| 연결 테스트 세트 B — 외부 관리자 후보 | `admin13@seed-event.co.kr` | qwer1234 |
| 세트 C 제1 관리자 | `admin21@seed.mapo.go.kr` | qwer1234 |
| 세트 D 제1 관리자 | `admin31@seed.mapo.go.kr` | qwer1234 |
| 세트 E 제1 관리자 | `admin41@seed.mapo.go.kr` | qwer1234 |
| 세트 F 제1 관리자 | `admin51@seed.mapo.go.kr` | qwer1234 |
| 1번 축제 총괄(대량 계정) | owner.01@seed.mapo.go.kr | qwer1234 |
| 1번 축제 운영(대량 계정) | op.01@seed.mapo.go.kr | qwer1234 |
| 5번 축제 총괄(대량 계정) | owner.05@seed.mapo.go.kr | qwer1234 |
| 외부 운영 | contractor.01@seed-event.co.kr | qwer1234 |
| 1번 축제 스태프 | staff-01-01 | qwer1234 |
| 10번 축제 스태프 | staff-10-02 | qwer1234 |
| 비활성 스태프 | `staff-04-01` (id 7) | qwer1234 (실패 예상) |
| 삭제 스태프 | `staff-06-01` (id 11) | qwer1234 (실패 예상) |
| 기간 만료 스태프 | completed 축제의 `staff-{seed_idx}-01` | qwer1234 (실패 예상) |

---

## 14. SQL 실행 파일

**단일 통합 파일**: `시드데이터_RDS경량_통합.sql`

```bash
# 1) 파이프라인 선행
cd C:\projects\chookjibup\ChookJiBup_data_pipeline
python main.py

# 2) Admin 시드 (한 번에 실행)
psql "$DATABASE_URL" -f chookjibupAdmin_BE/docs/seed-data/시드데이터_RDS경량_통합.sql
```

포함 범위:

| 순서 | 내용 |
|------|------|
| 1 | `seed_festival_map` TEMP 생성 + 선삭제 |
| 2 | `admin_accounts` 48 + `admin_festival_roles` 40 |
| 3 | `field_staff_accounts` 20 |
| 4 | `festival_locations` 25 → `festival_maps` 13 → `festival_roadmap` 10 |
| 5 | `roadmap_node` 150 → `booth_info` 80 → 노드 UPDATE |
| 6 | `booth_queue` 80 + `booth_congestion` 320 |
| 7 | 방문 인원 + 시퀀스 `setval` |

재생성: `_generate_seed_sql.ps1` (코어: `_seed_sql_core.sql`). 생성 스크립트는 core 존재, placeholder 제거, BEGIN/COMMIT 및 필수 섹션을 검사한다. `map_analysis_job`은 기본 시드에 포함하지 않는다.

---

## 15. 이전 문서

- 이전 소량 초안: `reference/2026-08/초안_시드데이터_운영자10명이상.md`
