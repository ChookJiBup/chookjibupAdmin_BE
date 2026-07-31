# chookjibupAdmin_BE

`ChookJiBup`의 관리자용 Spring Boot 백엔드 애플리케이션이다.
축제 운영자가 축제를 등록하고, 축제별 관리자 권한을 관리하며,
현장 스태프와 운영 대시보드, 결과 리포트 기능으로 확장하는 것을 목표로 한다.

## 프로젝트 방향

이 프로젝트는 단순 관리자 계정 시스템이 아니라 축제 단위 운영 권한을 중심으로 설계한다.
관리자 계정은 로그인 시점에 제1 관리자 또는 제2 관리자로 고정되지 않는다.
특정 축제를 생성한 계정은 그 축제의 총괄관리자가 되고,
총괄관리자가 초대한 기존 관리자 계정은 해당 축제의 운영자가 된다.

현장 스태프는 관리자 회원가입을 거치지 않는다.
축제 운영 기간에 맞춰 별도 계정을 발급받아 로그인하고,
줄끝라인 수정 같은 제한된 현장 기능만 수행한다.

## 주요 역할

| 역할 | 설명 |
| --- | --- |
| 총괄관리자 | 축제를 생성한 관리자. 축제 정보 수정, 운영자 초대/관리, 스태프 관리 권한을 가진다. |
| 운영자 | 총괄관리자가 특정 축제에 초대한 관리자 계정. 대부분의 운영 기능을 공유하지만 총괄관리자 전용 권한은 제한된다. |
| 현장 스태프 | 관리자 회원가입 없이 발급 계정으로 접속한다. 축제 기간 전후로만 유효하며 현장 최하위 기능만 수행한다. |

## 현재 구현 범위

- 관리자 회원가입, 로그인, JWT 발급
- 이메일 인증 흐름과 Redis 기반 인증 상태 저장
- 관리자 계정 상태 관리와 탈퇴 상태 변경
- 축제 생성 및 수정
- 축제 생성자를 총괄관리자로 연결
- 총괄관리자 기준 운영자 후보 조회, 초대한 운영자 조회
- 관리자 본인이 관리하는 축제 이력 조회
- 현장 스태프 생성, 조회, 삭제, 로그인
- 사용자 서버에서 조회할 진행 예정/진행 중/진행 완료 축제 목록 API
- 서버 간 통신용 HMAC 기반 내부 API 인증
- Querydsl 기반 Query Repository projection 조회
- Swagger/OpenAPI 설정
- GitHub Actions 기반 CI 테스트 검증

## 향후 확장 대상

- 운영자 초대/삭제 정책 고도화
- 축제 기본정보 수정 가능 필드와 행사 상태별 수정 제한
- 부스맵, 줄끝라인 좌표, 수정 이력 저장
- 실시간 혼잡도 조회와 운영 대시보드
- AI 운영 제안
- 진행 완료 축제 결과 리포트
- 축제 방문인원 입력과 리포트 반영
- 관리자 회원 정보 수정과 이메일/조직 재인증 정책

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Spring WebMVC
- Spring Security
- Spring Data JPA
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
admin/
auth/
dashboard/
festival/
operator/
report/
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
- 의미 있는 문자열은 가능한 VO로 감싼다.
- JPA VO는 `record`가 아니라 class 기반 `@Embeddable`로 작성한다.
- VO의 DB 컬럼명은 VO 내부가 아니라 소유 Entity의 `@AttributeOverride`에서 지정한다.
- `createdAt`, `updatedAt`은 `BaseTimeEntity` 상속으로 관리한다.
- 비밀번호는 평문이 아니라 hash VO로 저장한다.

## 인증과 보안

관리자 인증은 이메일, 비밀번호 기반 로그인 후 JWT를 발급한다.
JWT는 계정 인증을 의미하며, 축제별 총괄관리자/운영자 권한은 요청 처리 시 별도로 판단한다.

이메일 인증은 Redis 기반으로 인증 코드를 관리한다.
운영 secret은 Git에 커밋하지 않고 환경 변수 또는 secret manager로 주입한다.
로컬 개발용 secret은 `src/main/resources/application-secret.yml`에 두며 `.gitignore` 대상이다.

사용자 서버와 통신하는 내부 API는 API Key, timestamp, nonce, HMAC signature 기반 검증을 사용한다.
nonce는 Redis로 재사용을 방지한다.

## 로컬 실행

PostgreSQL과 Redis가 필요하다.
기본 설정은 `src/main/resources/application.yml`에 있으며,
로컬 secret은 `src/main/resources/application-secret.yml`에 작성한다.

`application-secret.yml`은 Git에 커밋하지 않는다.
필요한 키 예시는 `src/main/resources/application-secret.example.yml`을 기준으로 확인한다.

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

## CI

GitHub Actions는 `main` push, `main` 대상 pull request, 수동 실행에서 동작한다.
CI는 다음을 확인한다.

- 운영 Java 코드 변경 시 테스트 파일 변경 여부
- 변경된 테스트 파일의 테스트 어노테이션 존재 여부
- 전체 Gradle 테스트 통과 여부

CI가 실패하면 GitHub Actions 결과는 실패로 표시된다.
다만 push 자체는 GitHub Actions 실행 전에 완료되므로 실패한 CI가 push를 직접 차단하지는 않는다.

## 문서

세부 개발 규칙은 `docs/backend-guides/` 아래 문서를 기준으로 한다.

- `01_아키텍처_구조_가이드.md`: 패키지 구조와 Command/Query 분리
- `02_계층별_책임_가이드.md`: Service, Repository, Query Repository 책임
- `03_DTO_API_Swagger_예외_가이드.md`: DTO, API 응답, Swagger, ErrorCode
- `04_도메인_동시성_외부인프라_가이드.md`: Redis, 외부 연동, 동시성
- `05_테스트_Fixture_가이드.md`: 테스트 범위와 작성 규칙
- `06_구현품질_JPA_트랜잭션_빌드_가이드.md`: JPA, VO, Lombok, 설정, 의존성
- `07_CodingAgent_리뷰체크리스트_가이드.md`: JavaDoc과 리뷰 기준
- `08_서버간_통신_보안_가이드.md`: 내부 API 보안 규격

기능 명세 정리는 `docs/정리_Notion_기능명세서.md`에 보존한다.
작업 중 작성하는 계획서와 결과보고서는 `reference/YYYY-MM-DD/`에 작성하지만,
해당 디렉터리는 로컬 참조용이며 커밋 대상에서 제외한다.
