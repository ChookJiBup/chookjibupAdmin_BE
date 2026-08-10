# AGENTS.md

`chookjibupAdmin_BE`은 `ChookJiBup`의 관리자용 Spring Boot 애플리케이션이다.
작업을 시작하기 전에 요청이 관리자 기능, 공통 API 계약, 빌드/설정 중 어디에 해당하는지 먼저 확인한다.

## 공통 작업 원칙

- 요청을 충족하는 최소 변경만 수행한다.
- 관광, 축제, 동선, 예측, 관리자 화면/운영 데이터 흐름을 기준으로 변경 범위를 판단한다.
- 사용자 백엔드와 공유되는 API 계약이나 데이터 형식이 바뀌면 사용자 앱 영향도 함께 확인한다.
- 사용자가 문서 또는 보고서 작성을 요청하면 기본적으로 `reference/YYYY-MM/` 디렉터리에 Markdown 파일로 작성한다.
- 참조 문서 파일명은 한글로 작성하고 문서 성격을 접두어로 붙인다. 예: `계획서_관리자_로그인.md`, `결과보고서_관리자_로그인.md`.
- 참조 문서 서두에는 문서 형식을 명시한다. 예: `문서 형식: 계획서`, `문서 형식: 결과보고서`.
- 로컬 참조 문서와 실행 로그는 커밋 대상 코드에 남기지 않는다.
- 사용자가 만든 변경은 되돌리지 않는다.

## chookjibupAdmin_BE 작업 지침

- 코드 관련 세부 규칙은 이 파일에 중복 작성하지 않고 `docs/backend-guides/` 하위 문서를 기준으로 따른다.
- 작업을 시작하기 전에 변경 유형에 맞는 가이드 파일을 먼저 읽고, 해당 파일의 규칙을 코드 작성 기준으로 삼는다.
- 한 작업이 여러 영역에 걸치면 관련 세부 가이드를 모두 읽고 적용한다.
- 가이드에 없는 세부 구현은 기존 코드 구조와 Spring Boot 관례를 우선한다.

## 백엔드 세부 가이드 참조 규칙

- 패키지 구조, 도메인 위치, Command/Query 분리를 수정하기 전에는 `docs/backend-guides/01_아키텍처_구조_가이드.md`를 읽는다.
- Application Service, Repository wrapper Service, Facade, Aggregate, Repository, Query Repository 구조를 만들거나 수정하기 전에는 `docs/backend-guides/02_계층별_책임_가이드.md`를 읽는다. Service에서 Repository 접근이 필요하면 직접 접근하지 않고 해당 도메인의 Repository wrapper Service를 먼저 사용한다. Query Repository는 Entity 대신 조회 DTO projection을 반환하고 Querydsl을 기본으로 사용한다.
- VO를 만들거나 Entity에 embedding 하기 전에는 `docs/backend-guides/02_계층별_책임_가이드.md`와 `docs/backend-guides/06_구현품질_JPA_트랜잭션_빌드_가이드.md`를 읽는다.
- Controller, Request/Response DTO, Command DTO, API 응답, 예외 응답, Swagger/OpenAPI를 만들거나 수정하기 전에는 `docs/backend-guides/03_DTO_API_Swagger_예외_가이드.md`를 읽는다.
- 상태 전이, 동시성 제어, Redis, 메시지 브로커, 외부 API 연동을 만들거나 수정하기 전에는 `docs/backend-guides/04_도메인_동시성_외부인프라_가이드.md`를 읽는다.
- 서버 간 통신, internal API, HMAC 서명, API Key, nonce, secret rotation, 내부 API 보안을 만들거나 수정하기 전에는 `docs/backend-guides/08_서버간_통신_보안_가이드.md`를 읽는다.
- 테스트 또는 Fixture를 만들거나 수정하기 전에는 `docs/backend-guides/05_테스트_Fixture_가이드.md`를 읽고 단위/통합 테스트 필수 범위를 확인한다.
- Entity 매핑, JPA 설정, `BaseTimeEntity`, Lombok, 네이밍, 트랜잭션, 빌드 의존성, yml 설정, secret 설정을 수정하기 전에는 `docs/backend-guides/06_구현품질_JPA_트랜잭션_빌드_가이드.md`를 읽는다.
- JavaDoc을 작성하거나 코드 리뷰 체크리스트를 확인하기 전에는 `docs/backend-guides/07_CodingAgent_리뷰체크리스트_가이드.md`를 읽는다.
- 커밋을 만들기 전에는 `docs/commit-message-guide.md`를 읽고 메시지와 atomic commit 규칙을 따른다.

## 지침 우선순위

더 구체적인 위치의 지침이 더 우선한다.

1. 루트의 `AGENTS.md`는 저장소 전체에 적용된다.
2. 이 파일은 논리 프로젝트 `chookjibupAdmin_BE`의 모든 하위 파일에 적용된다.

지침이 서로 충돌하면, 수정하려는 파일에 더 가까운 위치의 `AGENTS.md`를 우선한다.
