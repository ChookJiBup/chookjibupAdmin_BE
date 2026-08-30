# chookjibupAdmin_BE

`ChookJiBup`의 관리자용 Spring Boot 백엔드 애플리케이션이다.
축제 단위 운영 권한을 중심으로, 관리자·운영자·현장 스태프가
축제 등록·부스맵·현장 운영(줄끝·혼잡)·대시보드·방문 인원·결과 리포트까지
사용할 수 있는 API를 제공한다.

## 프로젝트 방향

이 프로젝트는 단순 관리자 계정 시스템이 아니라 축제 단위 운영 권한을 중심으로 설계한다.
관리자 계정은 로그인 시점에 총괄관리자 또는 운영자로 고정되지 않는다.
특정 축제를 생성한 계정은 그 축제의 총괄관리자가 되고,
총괄관리자가 초대한 기존 관리자 계정은 해당 축제의 운영자(제2관리자)가 된다.

계정 종류는 공공기관(`GOVERNMENT`)과 외부업자(`CONTRACTOR`)로 나뉜다.
외부업자는 축제에 배정되면 제2관리자(`SUB_ADMIN`)와 동일한 권한 플래그를 갖는다.

현장 스태프는 관리자 회원가입을 거치지 않는다.
축제 운영 기간에 맞춰 별도 계정을 발급받아 로그인하고,
줄끝 위치 수정·혼잡 확인 같은 제한된 현장 기능만 수행한다.

## 주요 역할

| 역할 | 설명 |
| --- | --- |
| 총괄관리자 | 축제를 생성한 관리자. 축제 정보 수정·삭제, 하위 관리자 초대/관리, 스태프·맵·운영 권한을 가진다. |
| 운영자(제2관리자) | 총괄관리자가 특정 축제에 초대한 관리자 계정. 대부분의 운영 기능을 공유하지만 총괄관리자 전용 권한은 제한된다. |
| 외부업자 | 공공기관이 아닌 계정 종류. 축제 배정 시 제2관리자와 동일 권한으로 동작한다. |
| 현장 스태프 | 관리자 회원가입 없이 발급 계정으로 접속한다. 축제 기간 전후로만 유효하며 현장 최하위 기능만 수행한다. |

## 현재 구현 범위

### 인증·계정

- 관리자 회원가입(공공기관), 외부업자 회원가입, 로그인, JWT 발급(액세스 쿠키)
- 이메일 인증·비밀번호 재설정 흐름과 Redis 기반 상태 저장
- 관리자 본인 정보 조회/수정, 탈퇴 상태 변경
- 현장 스태프 생성·조회·수정·삭제·상태 변경·비밀번호 재발급·로그인

### 축제·권한

- 축제 생성(JSON/multipart)·수정·삭제
- 축제 생성자를 총괄관리자로 연결
- 관리 축제 목록/상세 조회(방문 인원 입력 방식 등 포함)
- 하위 관리자(운영자) 후보 조회·초대·조회·삭제
- 축제 시리즈 검색, 도로명 주소 검색, 축제 장소 조회
- 사용자 서버용 내부 축제 목록·장소 API (`/internal/api/**`)

### 부스맵·부스

- 축제 맵 업로드·편집·분석·조회 URL·삭제
- 부스 노드 승인 및 부스 정보 연결
- 대표 장소 좌표(한국 인근 범위) 검증

### 현장 운영

- 전체·부스별 혼잡도 조회 (`GET .../operations/congestion`)
- 부스별 대기열·줄끝 조회 (`GET .../operations/queues`)
- 줄끝 좌표·거리·경로 수정 (`PATCH .../operations/queues/{queueId}`)
- 줄끝 거리(`queueTailMeters`)가 있으면 동일 트랜잭션에서 혼잡·대기시간 추정 이력을 자동 append
  - 대기시간: `ceil(거리 / 10) × 10` 분
  - 등급: `0~10m LOW`, `11~30m MEDIUM`, `31m+ HIGH`
  - 직전 이력과 wait·level이 같으면 중복 저장하지 않음
- 수동 혼잡도 입력 유지 (`PUT .../booths/{boothId}/congestion`)
- 규칙 기반 운영 제안 조회 (`GET .../operations/suggestions`, OpenAI 연동 전 스텁)

### 대시보드·방문·리포트

- 축제 운영 대시보드(축제명, 부스 WGS84 좌표, 수정자 이름, 구역, 혼잡·대기 지표)
- 방문 인원 일별/총원 입력·조회
- 결과 리포트 요약·상태·생성·성과·평가 조회 API

### 기반

- Querydsl 기반 Query Repository projection 조회
- Flyway 마이그레이션(스키마 소유, JPA `ddl-auto=validate`)
- Swagger/OpenAPI, GitHub Actions CI 테스트 검증

## 주요 API 그룹

| 접두 경로 | 용도 |
| --- | --- |
| `/api/admin/**` | 관리자 인증, 마이페이지, 관리 축제 |
| `/api/field-staff/**` | 현장 스태프 로그인 |
| `/api/festivals/**` | 축제·맵·부스·운영·대시보드·방문·리포트·스태프·하위관리자 |
| `/api/festival-series/**` | 축제 시리즈 검색 |
| `/api/addresses/**` | 도로명 주소 검색 |
| `/internal/api/festivals/**` | 사용자 서버용 내부 조회 |

세부 계약은 실행 중 Swagger UI와 컨트롤러 DTO를 기준으로 한다.

## 향후 확장 대상

- 운영자 초대/삭제·탈퇴 시 권한 이전 정책 고도화
- 행사 상태별 축제 수정 가능 필드 제한 강화
- 운영 제안의 OpenAI(또는 외부 모델) 연동
- 혼잡 자동/수동 출처·계산 버전 감사 컬럼
- 줄끝 PATCH 응답에 혼잡 추정값 동시 반환(프런트 요구 시)
- 진행 완료 축제 결과 리포트 고도화(분석·내보내기)
- 실시간 푸시/스트리밍 혼잡 갱신

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Spring WebMVC
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Redis
- Querydsl
- Lombok
- springdoc-openapi
- JUnit 5, AssertJ, Mockito
- H2 test runtime

## 아키텍처 원칙

패키지는 기술 계층보다 도메인을 우선한다.
HTTP 진입점은 최상위 `api/` 아래에 도메인별로 모으고,
도메인 내부는 `command`와 `query`를 분리한다.

```text
api/
address/
admin/
auth/
booth/
dashboard/
festival/
map/
operator/
report/
visitor/
common/
global/
```

Command는 상태 변경, 트랜잭션, 도메인 규칙 실행을 담당한다.
Query는 조회 DTO projection, 검색, 필터링, 정렬, 페이징을 담당한다.
Query Repository 구현은 Querydsl을 기본으로 사용하고 Entity 대신 조회 DTO를 반환한다.

Service 네이밍은 책임에 맞춘다.
`[domain]Service`는 Repository wrapper Service로 제한하고,
실제 유스케이스 흐름은 `[domain][행위]Service` 또는 `[domain]ApplicationService`로 둔다.
wrapper Service를 제외한 Service는 Repository에 직접 접근하지 않는다.

Repository는 다음 세 층으로 나눈다.

```text
DomainRepository
DomainRepositoryImpl
DomainJpaRepository
```

## 도메인 모델 기준

- 내부 DB 식별자는 `Long id`를 사용한다.
- 외부 API에는 UUID 기반 public id를 우선 사용한다.
  - 예: 축제·대기열(`queueId`)은 UUID, 일부 부스 혼잡 PUT의 `boothId`는 내부 Long을 유지한다.
- 의미 있는 문자열은 가능한 VO로 감싼다.
- JPA VO는 `record`가 아니라 class 기반 `@Embeddable`로 작성한다.
- VO의 DB 컬럼명은 VO 내부가 아니라 소유 Entity의 `@AttributeOverride`에서 지정한다.
- `createdAt`, `updatedAt`은 `BaseTimeEntity` 상속으로 관리한다.
- 비밀번호는 평문이 아니라 hash VO로 저장한다.

## 인증과 보안

관리자 인증은 이메일, 비밀번호 기반 로그인 후 JWT를 발급한다.
발급 토큰은 주로 HTTP 전용 쿠키로 전달한다.
JWT는 계정 인증을 의미하며, 축제별 총괄관리자/운영자 권한은 요청 처리 시 별도로 판단한다.
현장 스태프는 별도 JWT 설정으로 인증한다.

이메일 인증은 Redis 기반으로 인증 코드를 관리한다.
운영 secret은 Git에 커밋하지 않고 환경 변수 또는 secret manager로 주입한다.
로컬 개발용 secret은 `src/main/resources/application-secret.yml`에 두며 `.gitignore` 대상이다.

사용자 서버가 호출하는 내부 API(`/internal/api/**`)는 애플리케이션 계층 HMAC 인증을 사용하지 않는다.
호출 신뢰는 네트워크 격리 등 인프라 계층에서 보장한다.

## 로컬 실행

PostgreSQL과 Redis가 필요하다.
루트 `docker-compose.yml`로 로컬 인프라를 띄울 수 있다.

```bash
docker compose up -d
```

기본 설정은 `src/main/resources/application.yml`에 있으며,
로컬 secret은 `src/main/resources/application-secret.yml`에 작성한다.
`application-secret.yml`은 Git에 커밋하지 않는다.
필요한 키 예시는 `src/main/resources/application-secret.example.yml`을 기준으로 확인한다.

스키마는 Flyway가 적용한다. 앱 기동 시 마이그레이션이 실행되며,
JPA는 기본적으로 `validate` 모드이다.

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용할 수 있다.

```powershell
.\gradlew.bat bootRun
```

## 테스트

전체 테스트는 다음 명령으로 실행한다.

```bash
./gradlew test
```

Windows PowerShell에서는 다음 명령을 사용할 수 있다.

```powershell
.\gradlew.bat test
```

테스트 작성 원칙은 `docs/backend-guides/05_테스트_Fixture_가이드.md`를 따른다.
Controller를 제외한 운영 클래스는 단위 테스트 대상이며,
Repository와 wrapper Service를 제외한 Service는 통합 테스트 대상이다.

## CI/CD

GitHub Actions는 `main` push, `main` 대상 pull request, 수동 실행에서 동작한다.
CI는 다음을 확인한다.

- 운영 Java 코드 변경 시 테스트 파일 변경 여부
- 변경된 테스트 파일의 테스트 어노테이션 존재 여부
- 전체 Gradle 테스트 통과 여부

CI가 실패하면 GitHub Actions 결과는 실패로 표시된다.
다만 push 자체는 GitHub Actions 실행 전에 완료되므로 실패한 CI가 push를 직접 차단하지는 않는다.

### 배포(CD)

`main` 브랜치 push에 대한 CI가 성공하면 `deploy.yml`이 `production` Environment에서
AWS Systems Manager(SSM)를 통해 EC2 배포를 실행한다. `workflow_dispatch`로 수동 배포할 때도
`main` 브랜치 실행만 허용한다.

GitHub 저장소의 `Settings → Environments → production`에 다음 값을 등록한다.

| 종류 | 이름 | 필수 여부 | 용도 |
| --- | --- | --- | --- |
| Secret | `AWS_ACCESS_KEY_ID` | 필수 | SSM 명령 및 Parameter Store 접근 |
| Secret | `AWS_SECRET_ACCESS_KEY` | 필수 | AWS 자격 증명 |
| Secret | `ADMIN_APPLICATION_SECRET_YML` | 필수 | 운영 `application-secret.yml` 전체 내용 |
| Variable | `ADMIN_EC2_INSTANCE_ID` | 필수 | 배포 대상 EC2 인스턴스 ID |
| Variable | `AWS_REGION` | 선택 | 기본값 `ap-northeast-2` |
| Variable | `ADMIN_DEPLOY_PATH` | 선택 | 기본값 `/home/ec2-user/app/chookjibupAdmin_BE` |
| Variable | `ADMIN_DEPLOY_USER` | 선택 | 기본값 `ec2-user` |
| Variable | `ADMIN_DEPLOY_HOME` | 선택 | 기본값 `/home/ec2-user` |
| Variable | `ADMIN_SYSTEMD_SERVICE` | 선택 | 기본값 `chookjibup-admin-backend` |
| Variable | `ADMIN_JAVA_HOME` | 선택 | EC2의 Java 21 경로 |
| Variable | `ADMIN_APPLICATION_SECRET_PARAMETER` | 선택 | 기본값 `/chookjibup/admin/application-secret-yml` |
| Variable | `ADMIN_APPLICATION_SECRET_FILE` | 선택 | EC2 secret 파일 경로 |
| Variable | `ADMIN_HEALTH_URL` | 선택 | 기본값 `http://127.0.0.1:8080/swagger-ui/index.html` |

`ADMIN_APPLICATION_SECRET_YML`에는 [application-secret.example.yml](src/main/resources/application-secret.example.yml)을
기준으로 작성한 YAML 전체를 넣는다. 값을 base64로 인코딩하거나 Git 저장소에
`application-secret.yml`을 커밋하지 않는다. 배포 시 workflow가 해당 내용을 SSM SecureString으로
저장하고, EC2에서 파일로 복원한 뒤 systemd 서비스를 재시작한다.

배포 대상 EC2는 SSM Agent가 온라인 상태여야 하며, 인스턴스 역할에 SSM Agent 통신과
Parameter Store 조회 권한이 있어야 한다. CI용 AWS 사용자는 Parameter Store 쓰기와
SSM 명령 전송·상태 조회 권한이 필요하다. 스키마 변경은 Flyway로만 반영되므로
운영 JPA 설정은 `validate`를 유지한다.

## 문서

세부 개발 규칙은 `docs/backend-guides/` 아래 문서를 기준으로 한다.

- `01_아키텍처_구조_가이드.md`: 패키지 구조와 Command/Query 분리
- `02_계층별_책임_가이드.md`: Service, Repository, Query Repository 책임
- `03_DTO_API_Swagger_예외_가이드.md`: DTO, API 응답, Swagger, ErrorCode
- `04_도메인_동시성_외부인프라_가이드.md`: Redis, 외부 연동, 동시성
- `05_테스트_Fixture_가이드.md`: 테스트 범위와 작성 규칙
- `06_구현품질_JPA_트랜잭션_빌드_가이드.md`: JPA, VO, Lombok, 설정, 의존성
- `07_CodingAgent_리뷰체크리스트_가이드.md`: JavaDoc과 리뷰 기준

커밋 메시지는 `docs/commit-message-guide.md`를 따른다.
에이전트/작업 규칙은 루트 및 본 디렉터리의 `AGENTS.md`를 따른다.

기능 명세 정리는 `docs/정리_Notion_기능명세서.md`에 보존한다.
작업 중 작성하는 계획서와 결과보고서는 `reference/YYYY-MM/`에 작성하지만,
해당 디렉터리는 로컬 참조용이며 커밋 대상에서 제외한다.
