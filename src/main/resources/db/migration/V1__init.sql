-- PawWalk Phase 1 (MVP) DB Schema — Flyway 베이스라인 마이그레이션
-- PawWalk-ios 레포의 DB_SCHEMA.sql을 기준 문서로 이관 (2026-07-09).
--
-- 설계 원칙:
--   - Phase 1 범위만 반영 (그룹 채팅/그룹 완료카드/성향매칭 알고리즘/시설탐색 등
--     Phase 2+ 기능은 컬럼도 추가하지 않음 — TICKETS_MVP.md "Phase 1 제외 항목" 참조)
--   - enum은 CHECK 제약으로 표현
--   - 권한 검증(RLS)은 안 씀 — Spring Boot Service 계층 코드에서 처리
--   - 위치 컬럼은 PostGIS geography(Point,4326) 사용
--     ⚠ PostGIS는 좌표 순서가 (경도 lng, 위도 lat)이다 — ST_MakePoint(lng, lat) 순서 주의
--   - Refresh Token은 Postgres가 아니라 Redis에 저장 (관련 테이블 없음)

CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================
-- 유저 / 반려견
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT, -- bcrypt 해시. Apple 로그인 전용 계정이면 NULL 가능
    apple_user_id TEXT UNIQUE, -- Sign in with Apple 고유 식별자(sub claim)
    display_name TEXT, -- 2026-07-09: NOT NULL이었으나 회원가입 시점엔 비워두고 온보딩에서
                        -- 채우는 설계(API_SPEC.md 0절)와 맞춰 nullable로 정정
    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')), -- 2026-07-09 추가, 보일러플레이트 Role 이식용
    profile_image_url TEXT,
    subscription_tier TEXT NOT NULL DEFAULT 'free' CHECK (subscription_tier IN ('free', 'plus', 'pro')),
    onboarding_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (password_hash IS NOT NULL OR apple_user_id IS NOT NULL)
);

CREATE TABLE dogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    breed TEXT,
    birth_date DATE,
    gender TEXT CHECK (gender IN ('male', 'female')),
    size TEXT CHECK (size IN ('small', 'medium', 'large')), -- MAP-2 셀프 필터
    energy_level TEXT CHECK (energy_level IN ('low', 'medium', 'high')), -- MAP-2 셀프 필터
    neutered BOOLEAN,
    profile_image_url TEXT,
    animal_registration_number TEXT, -- 표시용만, API 연동 없음
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 서비스 지역 게이팅 (ONBOARD-2)
-- ============================================================

CREATE TABLE service_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    center_location GEOGRAPHY(POINT, 4326) NOT NULL,
    radius_meters INTEGER NOT NULL DEFAULT 2000,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'planned')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_areas_location ON service_areas USING GIST (center_location);

CREATE TABLE waitlist_signups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    nearest_service_area_id UUID REFERENCES service_areas (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 매칭 (MAP-2)
-- ============================================================

CREATE TABLE walk_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_dog_id UUID NOT NULL REFERENCES dogs (id) ON DELETE CASCADE,
    service_area_id UUID NOT NULL REFERENCES service_areas (id),
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    size_filter TEXT CHECK (size_filter IN ('small', 'medium', 'large')),
    age_filter TEXT,
    energy_filter TEXT CHECK (energy_filter IN ('low', 'medium', 'high')),
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'matched', 'closed', 'completed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_walk_posts_location ON walk_posts USING GIST (location);

CREATE TABLE walk_post_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walk_post_id UUID NOT NULL REFERENCES walk_posts (id) ON DELETE CASCADE,
    requester_dog_id UUID NOT NULL REFERENCES dogs (id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ
);

-- 매칭 성사(수락) 시 생성되는 관계. 실시간 위치 공유 + 채팅 권한의 기준.
CREATE TABLE connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_a_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_b_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source_walk_post_id UUID REFERENCES walk_posts (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);

-- ============================================================
-- 채팅 (Phase 1은 1:1만)
-- ============================================================

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_id UUID NOT NULL REFERENCES connections (id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users (id),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 푸시 알림 디바이스 토큰
-- ============================================================

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    apns_token TEXT NOT NULL,
    platform TEXT NOT NULL DEFAULT 'ios' CHECK (platform IN ('ios')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, apns_token)
);

-- ============================================================
-- 산책 기록 & 완료 카드
-- ============================================================

CREATE TABLE walk_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dog_id UUID NOT NULL REFERENCES dogs (id) ON DELETE CASCADE,
    walk_post_id UUID REFERENCES walk_posts (id),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    distance_meters NUMERIC,
    route JSONB, -- [{lat, lng, ts}, ...] — 포인트별 시각이 필요해 PostGIS LINESTRING 대신 JSONB 사용
    weather_condition TEXT,
    weather_temp_celsius NUMERIC,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE walk_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walk_session_id UUID NOT NULL REFERENCES walk_sessions (id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    template_style TEXT NOT NULL DEFAULT 'default',
    shared_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 반려동물 케어
-- ============================================================

CREATE TABLE vaccination_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dog_id UUID NOT NULL REFERENCES dogs (id) ON DELETE CASCADE,
    vaccine_type TEXT NOT NULL,
    administered_date DATE NOT NULL,
    next_due_date DATE,
    reminder_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 안전: 차단 & 신고
-- ============================================================

CREATE TABLE blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reported_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason TEXT NOT NULL CHECK (
        reason IN ('inappropriate_behavior', 'safety_concern', 'harassment', 'fake_profile', 'other')
    ),
    details TEXT,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'reviewed', 'action_taken', 'dismissed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (reporter_id <> reported_user_id)
);

-- ============================================================
-- 구독 (수익 모델)
-- ============================================================

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    tier TEXT NOT NULL CHECK (tier IN ('plus', 'pro')),
    apple_transaction_id TEXT UNIQUE,
    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'expired', 'cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
