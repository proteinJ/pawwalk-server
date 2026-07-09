# pawwalk-server

PawWalk 백엔드. [spring-boot-boilerplate](https://github.com/proteinJ/spring-boot-boilerplate)에서
생성, 인증/에러처리 인프라는 그대로 두고 PawWalk 전용 설정(패키지명, PostGIS, Flyway)을 추가했습니다.

기획/API 설계 문서는 `PawWalk-ios` 레포의 `PAWWALK_CONCEPT_V2_DRAFT.md`,
`TICKETS_MVP.md`, `docs/api/API_SPEC.md`, `DB_SCHEMA.sql` 참조.

## 스택

- Java 17, Spring Boot 3.5, Gradle
- Spring Security + JWT (Access/Refresh Token, Redis 저장)
- Spring Data JPA + PostgreSQL (Supabase 호스팅) + **PostGIS** (위치 기반 매칭)
- Flyway (스키마 마이그레이션, `src/main/resources/db/migration/`)
- Redis (Refresh Token 저장, 로그아웃 블랙리스트)
- Swagger UI (SpringDoc OpenAPI)

## 시작하기

1. 환경변수 설정

```
DB_URL=jdbc:postgresql://<Supabase 프로젝트 호스트>:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=<Supabase DB 비밀번호>
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=<충분히 긴 랜덤 문자열>
```

2. Supabase 프로젝트 대시보드 → Database → Extensions에서 **postgis 활성화**
   (Flyway 마이그레이션이 `CREATE EXTENSION IF NOT EXISTS postgis`를 실행하긴 하지만,
   Supabase 관리형 환경에서는 대시보드에서 먼저 켜두는 걸 권장)
3. 로컬 Redis 실행 (`brew services start redis` 또는 Docker)
4. 실행 — `./gradlew bootRun` (기동 시 Flyway가 `V1__init.sql`을 자동 적용)

## API

Swagger UI: `/swagger-ui/index.html` (전체 API 문서 + JWT Authorize 테스트)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (AT/RT 발급) |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| GET | `/api/v1/members/me` | 내 정보 조회 |
| PATCH | `/api/v1/members/me/password` | 비밀번호 변경 |
| DELETE | `/api/v1/members/me` | 회원 탈퇴 (Apple 심사 가이드라인 5.1.1(v) 대응) |

나머지 도메인(dogs, walk-posts, chat, feed, walk-cards 등)은
`PawWalk-ios/docs/api/API_SPEC.md` 명세대로 구현 예정 — 아직 미구현.

## 스키마 관련 참고

- `users` 테이블 PK는 `UUID`(boilerplate 원본은 `Long`이었음 — DB_SCHEMA.sql과
  맞추기 위해 전환)
- `role` 컬럼은 boilerplate의 인증 패턴 유지를 위해 추가(DB_SCHEMA.sql 원본에는 없었음)
- `display_name`은 nullable로 조정 — 회원가입 시점엔 비워두고 온보딩에서 채우는 설계

## 아직 안 된 것

- Sign in with Apple (JwtProvider.createTokenForSocial()에 이어붙일 자리만 있음)
- dogs, walk-posts, chat, feed, walk-cards, vaccination-records, service-areas,
  subscriptions, blocks/reports 도메인 전부 미구현
- 이미지 저장소, 날씨 API 연동 방식 미정
