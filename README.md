# spring-boot-boilerplate

Spring Boot 3.5 (Java 17, Gradle) 기반 범용 백엔드 스타터 템플릿.
새 프로젝트를 시작할 때 인증/에러처리를 매번 새로 짜지 않도록 만든 재사용 템플릿입니다.

## 스택

- Java 17, Spring Boot 3.5, Gradle
- Spring Security + JWT (Access/Refresh Token)
- Spring Data JPA + PostgreSQL
- Redis (Refresh Token 저장, 로그아웃 블랙리스트)

## 기능

- **인증**: 이메일/비밀번호 회원가입·로그인·로그아웃, JWT 토큰 발급/갱신
  - Refresh Token은 Redis에 저장(TTL 7일)
  - 로그아웃 시 Access Token은 Redis 블랙리스트로 등록
  - 토큰 갱신 시 Refresh Token 회전(재사용 방지)
- **에러 처리**: `BusinessException` + `ErrorCode`(enum) + `GlobalExceptionHandler`로
  전역 예외를 일관된 포맷으로 응답
- **공통 응답 포맷**: `ApiResponse<T>` — 모든 API가 `success`/`data`/`message`/`code`로 통일
- **Swagger UI**: 실행 후 `/swagger-ui/index.html` — "Authorize" 버튼에 로그인으로 받은
  accessToken을 넣으면 보호된 API도 UI에서 바로 테스트 가능

## 시작하기

1. 패키지명(`com.proteinj.boilerplate`)을 프로젝트에 맞게 변경 (IDE Rename Package)
2. 환경변수 설정 (`.env` 또는 실행 환경변수)

```
DB_URL=jdbc:postgresql://localhost:5432/yourdb
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=<충분히 긴 랜덤 문자열>
```

3. PostgreSQL, Redis를 로컬에 띄운 뒤 실행

```
./gradlew bootRun
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (AT/RT 발급) |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| GET | `/api/v1/members/me` | 내 정보 조회 |
| PATCH | `/api/v1/members/me/password` | 비밀번호 변경 |
| DELETE | `/api/v1/members/me` | 회원 탈퇴 (Apple 심사 가이드라인 5.1.1(v) 대응) |

## 새 프로젝트에 쓸 때

- `ErrorCode`에 도메인별 에러코드를 이어서 추가
- 소셜 로그인이 필요하면 `JwtProvider.createTokenForSocial()`을 시작점으로 확장
- WebSocket, 공간 데이터(PostGIS) 등은 포함되어 있지 않음 — 프로젝트 성격에 따라 추가
