문서 형식: 운영 가이드

# RDS 경량 시드 데이터

Admin BE의 개발·연동 테스트용 경량 시드 세트다.

## 실행 전제

- `ChookJiBup_data_pipeline`이 `festivals`와 파이프라인 전용 테이블을 먼저 적재해야 한다.
- 관리자 계정·스태프·지도·부스·방문 입력 테이블은 Admin BE Flyway(V1~V16)가 생성해야 한다.
- 파이프라인 `schema.sql`의 `admins`·`festival_staff` 테이블을 같은 대상에 중복 적용하지 않는다.
- 운영 데이터가 있는 RDS에서는 실행 전 백업과 관리자 ID `910001..910048` 예약 범위 확인이 필요하다.

## 파일 역할

| 파일 | 용도 |
|------|------|
| `시드데이터_RDS경량_파이프라인연동.md` | 수량·관계·검증 기준 명세 |
| `검토보고서_시드데이터_RDS경량_정합성.md` | 정합성 검토와 실행 후 검증 쿼리 |
| `_seed_sql_core.sql` | placeholder를 포함한 생성 원본. 직접 실행 금지 |
| `_generate_seed_sql.ps1` | 실행용 통합 SQL 생성기 |
| `시드데이터_RDS경량_통합.sql` | placeholder 치환이 완료된 실행 파일 |
| `rds-light-seed.sql` | Actions/EC2용 ASCII 경로 별칭(통합 SQL과 동일) |

## 생성·실행

### 로컬 / 직접 psql

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\docs\seed-data\_generate_seed_sql.ps1
psql -f .\docs\seed-data\시드데이터_RDS경량_통합.sql
```

생성기는 core 파일 존재, placeholder 제거, `BEGIN`/`COMMIT`, 필수 SQL 섹션을 검사한다. 실제 RDS 실행 후에는 검토보고서의 검증 쿼리를 수행한다.

### GitHub Actions (1회성 RDS 시드)

CI(`./gradlew test`)에는 시드를 넣지 않는다. 실 RDS용 시드는 Actions
**Seed RDS**를 수동 1회 실행한다. EC2에 `psql` 설치는 필요 없고
`./gradlew applyRdsSeed`(JDBC)를 사용한다.

성공 후 워크플로/시드 러너는 삭제해도 된다. 상세는
`.github/DEPLOYMENT.md`의 **RDS 경량 시드 (1회성 수동)**.

시드 성공 후 공무원 로그인: `admin01@seed.mapo.go.kr` / `qwer1234`
