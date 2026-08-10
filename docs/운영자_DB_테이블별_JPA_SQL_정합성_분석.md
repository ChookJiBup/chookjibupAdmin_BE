# 관리자 BE 테이블별 JPA·SQL 정합성 분석

## 1. 분석 범위

이 문서는 `chookjibupAdmin_BE`가 직접 사용하는 운영자 DB 테이블만 분석한다.

분석 목표:

- 현재 SQL에 JPA를 맞춰야 하는 필수 변경
- 기존 관리자 기능을 유지하기 위해 SQL을 보완해야 하는 변경
- 당장 적용하지 않아도 되는 장기 개선

기준:

- `reference/2026-08/운영자DB_SQl.sql`
- 현재 관리자 BE JPA와 API
- 기준일 2026-08-02

### 제외 대상

| 테이블 | 제외 이유 |
| --- | --- |
| `users` | 사용자 BE 소유. 관리자 BE에서 사용하지 않음 |
| `festival_wishlist` | 사용자 BE 소유 |
| `festival_review` | 사용자 BE 소유. 관리자는 집계 결과만 사용 |
| `festival_visitor_excel` | 데이터 파이프라인 소유 |

관리자 BE에는 위 테이블의 Entity와 Command Repository를 만들지 않는다.

---

## 2. 판단 원칙

JPA와 SQL은 서로 대체 관계가 아니다.

```text
SQL
  -> 실제 테이블, 컬럼, FK, UNIQUE, CHECK, 인덱스의 최종 기준

JPA
  -> SQL에 매핑되는 관리자 업무 Aggregate

Application Service
  -> 권한 확인, 상태 전이, 여러 Aggregate 작업 순서
```

현재 운영 DB와 보존 데이터가 없으므로 기존 데이터 migration은 우선순위가 아니다. 먼저 생성 SQL을 목표 구조로 수정하고 빈 PostgreSQL DB에서 검증한다. Flyway baseline은 실제 운영 DB를 만들기 전에 적용한다.

### 테이블별 판정

| 판정 | 의미 |
| --- | --- |
| SQL 우선 | 현재 SQL 구조를 유지하고 JPA가 맞춤 |
| JPA 우선 | 현재 관리자 업무 모델이 더 정확해 SQL을 보완 |
| 혼합 | SQL 관계와 JPA 업무 필드를 병합 |
| Query 전용 | 관리자 BE가 상태를 소유하지 않고 조회 projection만 사용 |

---

## 3. 전체 판정

| 테이블 | 판정 | 1차 적용 방향 |
| --- | --- | --- |
| `admins` | 혼합, JPA 업무 필드 우선 | SQL 확장 후 `AdminAccount`를 `admins`에 매핑 |
| `admin_email_verification` | Redis 우선 | SQL Entity를 만들지 않고 저장소 역할 확정 |
| `festivals` | 혼합 | SQL 공유 마스터 유지, 관리자 필드 추가 |
| `festival_dashboard` | SQL 우선 | OWNER 관계로 매핑하되 이름의 의미를 코드에서 분리 |
| `festival_operator_invite` | 혼합 | DB 제약 유지, 상태 전이는 Application에서 처리 |
| `festival_operator` | SQL 우선 | SUB_ADMIN 관계로 매핑 |
| `festival_staff` | 혼합, JPA 규칙 우선 | SQL 이름 유지 + UUID·유효기간·상태 병합 |
| `festival_roadmap` | 혼합 | 현재 캔버스 역할 유지, 파일 이력은 별도 구조 검토 |
| `roadmap_icon_type` | SQL 우선 | 카탈로그 조회 중심 |
| `roadmap_icon_placement` | JPA/화면 요구 우선 | SQL에 크기·순서·버전 추가 |
| `booth_info` | 혼합 | SQL 기본 구조 유지 + 관리자 CRUD 식별자 보완 |
| `festival_congestion` | 정책 확정 필요 | 이력형 또는 현재 상태형 결정 후 매핑 |
| `booth_congestion` | SQL 우선 | CHECK 유지, 입력 Application 추가 |
| `festival_visitor_count` | SQL 우선·Query 중심 | 대시보드 projection 사용 |
| `festival_result` | SQL 우선·Query 중심 | JSONB version 보완 |

---

## 4. 테이블별 상세 분석

## 4.1 `admins`

### SQL과 현재 JPA 차이

| 항목 | SQL `admins` | JPA `AdminAccount` |
| --- | --- | --- |
| 테이블 | `admins` | `admin_accounts` |
| PK | `admin_id` | `id` |
| 공개 UUID | 없음 | `public_id` |
| 이름 길이 | 50 | 100 |
| 조직 | 없음 | 필수 `organization` |
| 부서 | nullable | 필수 `department` |
| 직급 | 없음 | 필수 `job_rank` |
| 생년월일 | 선택 | 없음 |
| 상태 | `is_withdrawn` | ACTIVE/SUSPENDED/DELETED |
| 생성 시각 | `joined_at` | `created_at` |
| 탈퇴 시각 | `withdrawn_at` | 없음 |

### 판정: 혼합, JPA 업무 필드 우선

다른 SQL 테이블이 모두 `admins.admin_id`를 참조하므로 테이블명과 PK는 SQL을 따른다. 반면 관리자 가입 API가 조직·부서·직급을 필수로 받고 UUID와 정지 상태도 사용하므로 해당 JPA 필드는 SQL에 반영한다.

### 1단계: 현재 SQL에 맞출 JPA 수정

- `@Table(name = "admins")`
- PK에 `@Column(name = "admin_id")`
- `joined_at`, `updated_at`, `withdrawn_at` 매핑
- `birth_date`를 실제 사용할지 결정 후 선택적 매핑
- 모든 운영 FK가 내부 `admin_id`를 사용하도록 Repository 정리

### 2단계: 기존 관리자 기능 유지를 위한 SQL 수정

```sql
public_id UUID NOT NULL UNIQUE
organization VARCHAR(255) NOT NULL
job_rank VARCHAR(50) NOT NULL
status VARCHAR(30) NOT NULL
```

- `name`을 VARCHAR(100)으로 확장
- `department`를 NOT NULL로 변경
- `status='DELETED'`와 `withdrawn_at` 일관성 CHECK 추가
- `is_withdrawn`과 status가 중복되므로 최종적으로 status 하나로 통일

### 3단계: 장기 개선

- 관리자 상태를 PostgreSQL enum으로 만들지 VARCHAR+CHECK로 둘지 통일
- 변경 감사 로그가 필요하면 별도 audit 테이블 도입

### 검증

- 이메일·UUID 동시 중복 가입
- SUSPENDED 로그인 차단
- 탈퇴 시 status와 withdrawn_at 원자적 변경

---

## 4.2 `admin_email_verification`

### SQL과 현재 코드 차이

- SQL은 인증 코드를 PostgreSQL에 보관한다.
- 현재 관리자 BE는 Redis TTL Repository를 사용한다.
- 두 저장소가 동시에 유효 인증 상태를 가지면 어느 값이 최신인지 불명확하다.

### 판정: Redis 우선

인증 코드는 짧은 TTL, 일회 사용, 빠른 만료가 핵심이므로 Redis가 더 적합하다.

### 1단계: JPA 수정

- `admin_email_verification` JPA Entity를 만들지 않는다.
- 현재 `AdminEmailVerificationRepository` Port와 Redis 구현을 유지한다.

### 2단계: SQL 수정

다음 중 하나를 선택한다.

권장:

- 유효 인증 코드 테이블은 제거
- 감사가 필요하면 코드 원문이 없는 `admin_email_verification_audit`로 변경

감사 테이블에는 email, purpose, 요청·성공 시각, 결과, 시도 횟수만 저장하고 code 원문은 저장하지 않는다.

### 검증

- TTL 만료
- 성공 후 재사용 차단
- 시도 횟수 제한
- Redis 장애 시 실패 정책

---

## 4.3 `festivals`

### SQL과 현재 JPA 차이

| 항목 | SQL | 관리자 JPA/API |
| --- | --- | --- |
| PK | `festival_id` | `id` |
| 공개 식별자 | 없음 | UUID |
| 이름 | `festival_name` | `name` |
| 설명 | `content` | `description` |
| 주소 | `road_address` | `address` |
| 상세주소 | 없음 | 있음 |
| 운영 시간 | 없음 | 시작·종료 시간 |
| 회차 | 없음 | `festival_series` |
| 출처 | API/manual | 관리자 수동 등록 중심 |
| 진행 상태 | upcoming/ongoing/completed | 별도 계산 로직 |
| 편집 상태 | 없음 | DRAFT |

### 판정: 혼합

`festivals`는 사용자 BE와 파이프라인도 참조하는 공유 마스터이므로 SQL 구조를 버리고 관리자 전용 테이블로 바꾸면 안 된다. 관리자 API에 필요한 필드는 source가 manual인 행에 추가한다.

### 1단계: 현재 SQL에 맞출 JPA 수정

- PK를 `festival_id`에 매핑
- `name` → `festival_name`
- `description` → `content`
- `address` → `road_address`
- `sourceType`, `createdByAdminId`, `progressStatus` 매핑
- 수동 생성 시 `source_type=manual`, 생성 관리자 필수
- API 축제와 수동 축제 수정 권한 분리

### 2단계: 기존 관리자 기능 유지를 위한 SQL 수정

```sql
public_id UUID NOT NULL UNIQUE
detail_address VARCHAR(100)
operation_start_time TIME
operation_end_time TIME
publication_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
published_at TIMESTAMPTZ
series_id BIGINT
```

- `progress_status`와 `publication_status`를 합치지 않는다.
- API 행은 운영 시간이 없을 수 있으므로 전체 NOT NULL 대신 manual 행 CHECK를 사용한다.
- `festival_series` 기능을 유지한다면 series 테이블과 FK를 생성 SQL에 추가한다.

### 관리자·사용자 식별자 전환

- DB 내부 FK는 BIGINT 유지
- 관리자 API UUID 유지
- 사용자 BE는 현재 Long API를 사용하므로 즉시 UUID로 일괄 변경하지 않음
- 필요 시 UUID와 BIGINT 조회를 일정 기간 병행

### 장기 개선

- 파이프라인의 동적 `visitor_YYYY_*` 컬럼을 별도 통계 테이블로 분리
- API 원천의 안정적인 외부 식별자 확보

### 검증

- API loader가 manual 행을 수정하지 않음
- DRAFT 행 사용자 노출 차단
- 수동 축제 필수값 CHECK
- UUID와 내부 ID 조회 일치

---

## 4.4 `festival_dashboard`

### 현재 SQL과 JPA

- SQL은 축제당 한 명의 `admin_id`를 저장한다.
- 현재 JPA는 `AdminFestivalRole.FESTIVAL_OWNER`로 책임자를 표현한다.
- 관리자 대시보드 자체는 여러 조회를 조합한 DTO다.

### 판정: 물리 구조는 SQL 우선, 코드에서는 OWNER 역할로 추상화

현재 단계에서 `festival_dashboard`를 삭제하거나 역할 테이블로 즉시 통합하는 것은 범위가 크다. SQL은 유지하고 Repository 계층에서 이 행을 OWNER 역할로 해석하는 것이 최소 변경이다.

### 1단계: JPA 수정

- `FestivalDashboardOwner`처럼 실제 의미가 드러나는 내부 모델 또는 Repository 작성
- `festival_dashboard.admin_id`를 OWNER 권한으로 변환
- Controller와 대시보드 Query는 물리 테이블 이름을 알지 않게 함

### 2단계: SQL 수정

- 현재 FK와 축제당 1행 PK 유지
- 테이블 이름이 오해를 만든다면 운영 DB 도입 전 `festival_owner`로 rename 검토
- 대시보드 설정 테이블이 필요하면 owner 관계와 분리

### 검증

- 축제당 OWNER 한 명
- OWNER만 제2관리자 초대 가능
- 관리자 한 명이 여러 축제를 소유할 수 있음

---

## 4.5 `festival_operator_invite`

### 현재 SQL과 JPA

SQL은 초대 상태, 본인 초대 금지, pending 중복 부분 UNIQUE와 자동 운영자 생성 트리거를 제공한다. 현재 JPA에는 독립 초대 Aggregate가 없다.

### 판정: 혼합

동시성 제약은 SQL이 더 강하고, 상태 전이와 권한 검증은 Application/JPA가 더 명확하다.

### 1단계: JPA 수정

- `FestivalOperatorInvite` Aggregate 생성
- `accept`, `reject`, `cancel` 도메인 메서드
- inviter가 해당 축제 OWNER인지 검증
- 비관적 또는 낙관적 락으로 동시 수락·취소 제어

### 2단계: SQL 수정

- 본인 초대 CHECK 유지
- pending 부분 UNIQUE 유지
- `expires_at`, `version` 추가 검토
- 자동 `festival_operator` 생성 트리거는 Application과 책임이 중복되므로 제거 권장

트리거를 유지한다면 Application은 operator INSERT를 하지 않고 상태 변경 후 생성 결과만 조회해야 한다.

### 검증

- 중복 pending 동시 요청
- 수락과 취소 동시 요청
- 완료 초대 재수락 금지
- 본인 초대 금지

---

## 4.6 `festival_operator`

### 현재 SQL과 JPA

- SQL은 수락된 추가 운영자의 관리자·축제 N:M 관계다.
- 현재 JPA의 `SUB_ADMIN` 역할과 의미가 같다.

### 판정: SQL 우선

현재 SQL을 유지하면서 Application에서 `festival_operator` 행을 SUB_ADMIN으로 해석하면 된다. DB 테이블 통합은 장기 개선으로 미룬다.

### 1단계: JPA 수정

- `FestivalOperator` Entity 또는 얇은 Repository 모델 작성
- `(admin_id, festival_id)` 관계를 SUB_ADMIN 권한으로 변환
- 기존 `AdminFestivalRoleService`가 owner와 operator 저장소를 조합하도록 수정

### 2단계: SQL 수정

- 현재 UNIQUE와 FK 유지
- API 공개 식별자가 필요하면 `public_id UUID UNIQUE` 추가
- `invite_id`가 필수인지 정책 확정

### 장기 개선

OWNER와 SUB_ADMIN 조회 복잡성이 커질 때만 단일 `festival_admin_role` 테이블 통합을 검토한다.

### 검증

- 동일 관리자·축제 중복 배정 금지
- 초대 수락 전 SUB_ADMIN 권한 없음
- operator 삭제 후 권한 즉시 제거

---

## 4.7 `festival_staff`

### SQL과 현재 JPA 차이

| 항목 | SQL | JPA `FieldStaffAccount` |
| --- | --- | --- |
| 테이블 | `festival_staff` | `field_staff_accounts` |
| PK | `staff_id` | `id` |
| UUID | 없음 | 있음 |
| login ID | 전역 UNIQUE | 축제별 UNIQUE |
| 유효기간 | 없음 | 필수 |
| 상태 | is_active | ACTIVE/DELETED |
| 생성 관리자 | 있음 | 없음 |
| 생년월일 | 있음 | 없음 |

### 판정: 혼합, 현재 JPA 업무 규칙 우선

스태프 로그인 요청이 축제 UUID와 login ID를 함께 받으므로 login ID는 축제 단위 UNIQUE가 자연스럽다. 축제 기간에만 로그인시키는 유효기간도 보안상 유지해야 한다.

### 1단계: 현재 SQL에 맞출 JPA 수정

- `@Table(name="festival_staff")`
- PK를 `staff_id`에 매핑
- `createdByAdminId` 추가
- 선택적 `birthDate` 정책 결정

### 2단계: 기존 기능 유지를 위한 SQL 수정

```sql
public_id UUID NOT NULL UNIQUE
valid_from TIMESTAMPTZ NOT NULL
valid_until TIMESTAMPTZ NOT NULL
status VARCHAR(30) NOT NULL
updated_at TIMESTAMPTZ NOT NULL
UNIQUE(festival_id, login_id)
CHECK(valid_from <= valid_until)
```

- 전역 login ID UNIQUE 제거
- `is_active`와 status 중 하나로 통일

### 검증

- 다른 축제 동일 login ID 허용
- 같은 축제 중복 차단
- 유효기간 전·후 로그인 차단
- 생성 관리자의 축제 권한 확인

---

## 4.8 `festival_roadmap`

### SQL과 현재 작업 코드 차이

- SQL은 축제당 현재 roadmap 한 행과 `base_image_url`만 관리한다.
- 배치도 작업 코드는 S3 source/display object, checksum, 파일 크기, 교체·삭제 상태를 관리한다.

### 판정: 혼합

현재 캔버스는 `festival_roadmap`, 파일 이력은 별도 asset 책임으로 분리하는 방향이 적합하다. 다만 이는 1차 JPA 정합성보다 이후 SQL 확장 작업이다.

### 1단계: 현재 SQL에 맞출 JPA 수정

- `FestivalRoadmap` Entity 추가
- PK를 축제 ID로 사용
- roadmap type, canvas 크기, 작성 관리자 매핑
- `base_image_url`에 만료 Presigned URL을 저장하지 않음

### 2단계: 배치도 기능 유지를 위한 SQL 수정

권장:

- `festival_roadmap`에는 현재 asset FK 저장
- 별도 `festival_roadmap_asset`에 object key와 이력 저장
- S3 원본·표시본·checksum·크기·상태·교체·삭제 시각 저장
- roadmap과 asset에 version 컬럼 추가

### 검증

- 축제당 current roadmap 하나
- 동시 이미지 교체
- DB 실패 시 S3 보상 삭제
- Presigned URL 만료 후 재발급

---

## 4.9 `roadmap_icon_type`

### 판정: SQL 우선

카탈로그 code UNIQUE, 활성 상태, 이미지 위치 구조가 적절하다.

### JPA 수정

- 관리자 편집 화면에서는 Query projection으로 활성 아이콘만 조회
- 카탈로그 관리 기능이 생길 때만 Command Entity 추가
- `code`를 업무 식별자로 사용하고 내부 PK는 외부에 노출하지 않음

### SQL 수정

- 정렬 필요 시 `sort_order`
- 비공개 S3면 URL 대신 object key
- seed SQL로 code 목록 버전 관리

### 검증

- 비활성 아이콘 신규 배치 차단
- 기존 배치의 비활성 아이콘 조회 유지

---

## 4.10 `roadmap_icon_placement`

### SQL과 관리자 화면 차이

현재 SQL은 X/Y, 회전, 라벨만 저장한다. 관리자 화면은 노드 너비·높이·종류와 배치 순서도 복원해야 한다.

### 판정: JPA/화면 요구 우선

현 SQL만으로는 저장 후 동일한 편집 화면을 복원할 수 없다.

### 1단계: JPA 수정

- roadmap 내부 `RoadmapNode` Entity 설계
- 위치·크기 VO
- move, resize, rotate, rename 행동 메서드
- roadmap version 기반 동시 편집 검증

### 2단계: SQL 수정

```sql
public_id UUID NOT NULL UNIQUE
node_type VARCHAR/ENUM NOT NULL
width NUMERIC NOT NULL
height NUMERIC NOT NULL
z_index INTEGER NOT NULL
style JSONB
locked BOOLEAN NOT NULL DEFAULT false
version BIGINT NOT NULL
```

- 좌표·크기 범위 CHECK
- node type별 icon/booth 관계 CHECK 검토

### 검증

- 저장 후 round-trip 화면 일치
- 음수 크기 차단
- 동시 편집 충돌
- 다른 축제 노드 접근 차단

---

## 4.11 `booth_info`

### 판정: 혼합

축제 FK, 이름·설명·위치라는 SQL 기본 구조는 유지한다. 관리자 CRUD의 외부 식별자와 삭제 정책은 보완해야 한다.

### JPA 수정

- 관리자 BE에 Booth Aggregate 추가
- festival ID 소유 검증
- public setter 대신 updateInfo/delete 행동 메서드

### SQL 수정

- `public_id UUID UNIQUE` 추가 권장
- 화면 순서가 필요하면 `sort_order`
- 운영 중 삭제 이력을 보존하려면 status 또는 deleted_at
- 축제 내 booth 이름 중복 정책 결정

### 검증

- 다른 축제 booth 수정 차단
- booth 삭제 시 placement SET NULL과 congestion CASCADE

---

## 4.12 `festival_congestion`

### 판정: 저장 정책 선결 필요

SQL에 PK가 별도로 있고 시간 컬럼도 있어 여러 행을 저장할 수 있지만 UPDATE 트리거도 있다. 다음 중 무엇인지 확정되지 않았다.

1. 변경마다 새 행을 추가하는 이력형
2. 축제당 현재 행 하나를 UPDATE하는 상태형
3. 현재 상태와 이력을 분리하는 혼합형

### 권장: 이력형

혼잡도 흐름과 결과 보고서가 필요하므로 변경마다 새 행을 추가하는 방식이 적합하다.

### SQL 수정

- 이력형 확정 시 기존 행 UPDATE 금지
- `updated_at`과 UPDATE 트리거 제거 검토
- `(festival_id, created_at DESC)` 인덱스
- 이벤트 중복 방지가 필요하면 UUID event ID

### JPA 수정

- 생성 전용 Aggregate
- 기존 행 변경 메서드 금지
- 최신 상태는 Query projection으로 조회

### 검증

- 최신값 조회
- 동시 기록 손실 없음
- 관리자 탈퇴 후 과거 이력 보존

---

## 4.13 `booth_congestion`

### 판정: SQL 우선

관리자·스태프 중 정확히 한 수정자만 허용하는 CHECK와 대기시간 범위는 DB에서 유지해야 한다.

### JPA 수정

- `createdByAdmin`, `createdByStaff` 생성 경로 분리
- 인증 principal에 따라 올바른 modifier type 설정
- 기존 이력 수정 금지 권장

### SQL 수정

- 이력형이면 `(booth_id, created_at DESC)` 인덱스
- 대기시간 필수 정책이면 NOT NULL
- append-only 확정 시 updated_at 제거 검토

### 검증

- 잘못된 modifier 조합 CHECK
- 음수 대기시간
- 다른 축제 staff의 booth 수정 차단

---

## 4.14 `festival_visitor_count`

### 판정: SQL 우선, Query 중심

축제·날짜 UNIQUE와 0 이상 CHECK가 일자별 집계에 적합하다. 관리자 BE에서 무거운 Command Aggregate를 만들 필요가 없다.

### JPA/Query 수정

- 대시보드와 결과 보고서는 Query projection 사용
- 대량 적재는 SQL upsert
- 혼잡도 enum은 명시적으로 변환

### SQL 수정

- 값 없는 행이 의미 없다면 `visitor_count NOT NULL`
- 데이터 출처가 여러 개면 source 컬럼 추가
- 수정 적재가 필요하면 updated_at 추가

### 검증

- 같은 날짜 upsert
- 음수 차단
- 축제 기간 밖 집계 정책

---

## 4.15 `festival_result`

### 판정: SQL 우선, Query 중심

축제당 결과 snapshot을 JSONB로 저장하는 구조는 관리자 결과 보고서에 적합하다. 사용자 리뷰 원본 Entity를 관리자 BE에 만들 필요가 없다.

### JPA/Query 수정

- Entity 관계로 JSON 내부를 풀지 않음
- JSON mapper 또는 Query projection 사용
- 응답 DTO와 저장 JSON 구조를 직접 동일 객체로 사용하지 않음

### SQL 수정

```sql
schema_version INTEGER NOT NULL
generator_version VARCHAR(50)
generation_status VARCHAR(30)
```

- 재생성 성공 시 generated_at 갱신
- 실패 시 이전 정상 결과 보존 정책

### 검증

- JSON version별 역직렬화
- 재생성 upsert
- 생성 실패 시 기존 결과 보존

---

## 5. SQL에 없지만 관리자 JPA에 존재하는 구조

## 5.1 `festival_series`

기존 축제와 다음 연도 축제를 묶는 관리자 기능을 유지한다면 SQL에 추가해야 한다.

권장 컬럼:

- series ID
- public UUID
- 이름
- normalized name UNIQUE
- 생성·수정 시각

API 축제는 series 매칭 전 NULL일 수 있고, 관리자 수동 축제는 생성 시 연결한다.

## 5.2 `admin_festival_roles`

현재 단계에서는 별도 물리 테이블을 만들지 않고 다음 두 SQL 관계를 하나의 권한 추상화로 조합한다.

```text
festival_dashboard -> FESTIVAL_OWNER
festival_operator  -> SUB_ADMIN
```

장기적으로 조회·제약이 복잡해질 때만 단일 역할 테이블 통합을 검토한다.

## 5.3 `field_staff_accounts`

별도 테이블을 유지하지 않고 `festival_staff`에 현재 JPA의 UUID·유효기간·상태 규칙을 병합한다.

## 5.4 `festival_maps`

별도 현재 배치도 테이블로 경쟁시키지 않는다. 이미지 파일 이력이 필요하면 `festival_roadmap_asset` 역할로 SQL에 추가한다.

---

## 6. 적용 순서

### 1단계: 생성 SQL 확정

- 관리자 대상 테이블만 목표 구조 결정
- 사용자·파이프라인 소유 테이블은 수정 범위에서 제외
- UUID, 시간, enum 매핑 정책 확정

### 2단계: 관리자 계정과 권한

- `admins`
- `festival_dashboard`
- `festival_operator_invite`
- `festival_operator`

### 3단계: 축제와 현장 계정

- `festivals`
- `festival_series`
- `festival_staff`

### 4단계: 로드맵

- `festival_roadmap`
- 파일 asset
- icon type
- placement
- booth

### 5단계: 운영·리포트 조회

- festival/booth congestion
- visitor count
- festival result

### 6단계: PostgreSQL 검증

- 빈 DB에 수정된 생성 SQL 실행
- 관리자 BE `ddl-auto=validate`
- PostgreSQL enum·TIMESTAMPTZ 매핑 테스트
- 전체 API 회귀 테스트

---

## 7. 최종 권고

- 관리자 BE에서 `users`, wishlist, review 원본 Entity를 만들지 않는다.
- SQL을 무조건 JPA에 맞추거나 JPA를 무조건 SQL에 맞추지 않는다.
- 관리자 API에서 이미 사용하는 UUID·조직·직급·유효기간·배치도 이력은 SQL에 보완한다.
- 공유 FK와 UNIQUE/CHECK는 SQL을 따른다.
- OWNER와 SUB_ADMIN은 물리 테이블을 당장 통합하지 않고 Application에서 하나의 권한 모델로 추상화한다.
- 운영 DB가 없는 현재는 데이터 migration보다 수정된 생성 SQL과 빈 PostgreSQL 검증이 우선이다.
- 운영 DB 도입 전에는 생성 SQL을 Flyway baseline으로 전환하고 Hibernate는 `validate`만 사용한다.
