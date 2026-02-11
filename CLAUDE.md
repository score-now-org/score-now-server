# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Score Now Server는 BetsAPI를 통해 실시간 축구 경기 데이터를 수집·관리하는 Spring Boot REST API 서버다. MySQL(관계형), MongoDB(문서형), Redis(캐시)를 함께 사용한다.

## Build & Run Commands

```bash
./gradlew clean build          # 빌드 (QueryDSL Q-class 자동 생성)
./gradlew bootRun              # 서버 실행 (localhost:8080)
./gradlew test                 # 전체 테스트
./gradlew test --tests "com.scorenow.scorenow_api.SomeTest"  # 단일 테스트
docker-compose up -d           # 인프라 실행 (MySQL:3306, MongoDB:27017, Redis:6380)
```

Java 17 필수. QueryDSL Q-class는 `build/generated/querydsl`에 생성된다.

## Architecture

**패키지 구조:** `com.scorenow.scorenow_api`

- `domain/{도메인}/` — 도메인별로 controller, service, repository, entity, dto 계층 분리
- `external/betsapi/` — BetsAPI 외부 연동 클라이언트 (`BetsApiClient`)
- `global/` — 공통 설정(config), 예외처리(exception), 베이스 엔티티, 상수

**핵심 도메인:** match, league, team, sport, player

**데이터 저장 전략:**
- **MySQL (JPA):** Match, League, Team, Sport, Player 등 관계형 엔티티
- **MongoDB:** MatchDetailDocument(경기 상세 통계), MatchLineupDocument(라인업)
- **Redis:** 리액티브 캐시

**엔티티 ID 규칙:** `"BETS" + sportId + externalId` 형식 (예: BETS111275660)

**스케줄러:** `@EnableScheduling` 활성화. InplayMatchDetectionScheduler(경기 시작 감지), MatchDetailScheduler(경기 상세 동기화)

## Key Patterns

- **API 응답:** 모든 엔드포인트는 `ApiResponse<T>`로 래핑 (`{success, data/error, timestamp}`)
- **예외처리:** `ErrorCode` enum → `BusinessException` → `GlobalExceptionHandler`에서 일괄 처리. 에러 메시지는 한국어
- **QueryDSL:** 복잡한 검색 조건은 `MatchRepositoryCustom` + `MatchRepositoryImpl`로 구현
- **BaseEntity:** `createdAt`, `updatedAt` 자동 관리 (JPA Auditing)
- **Lombok:** `@Getter`, `@Builder`, `@NoArgsConstructor` 등 적극 사용

## API Endpoints
관리자 API: `/api/admin/matches` (CRUD)
Swagger UI: `/swagger-ui/index.html`

## Configuration

`application.yml`에서 DB 접속 정보, BetsAPI 토큰/타임아웃 등 설정. Spring Security는 Swagger 경로만 공개, 나머지는 인증 필요 (현재 SecurityAutoConfiguration exclude 상태).
