CREATE SCHEMA IF NOT EXISTS gatieottae;
SET search_path TO gatieottae;


-- Member status enum type (create if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'member_status') THEN
        CREATE TYPE gatieottae.member_status AS ENUM ('ACTIVE','BLOCKED','DELETED');
    END IF;
END$$;


-- Member table (email optional, password-based signup supported)
CREATE TABLE IF NOT EXISTS member (
    id                BIGSERIAL PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL,                 -- 로그인 아이디(필수)
    password_hash     VARCHAR(255) NOT NULL,                 -- 비밀번호 해시(필수)
    email             VARCHAR(255),                          -- 선택
    name              VARCHAR(50)  NOT NULL,                 -- 실명/표시 이름
    nickname          VARCHAR(50),                           -- 닉네임(선택)
    profile_image_url TEXT,
    status            member_status NOT NULL DEFAULT 'ACTIVE',
    oauth_provider    VARCHAR(20),                           -- 예: 'KAKAO'
    oauth_subject     VARCHAR(100),                          -- 예: 카카오 user id
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3) 제약/인덱스
-- username은 반드시 유니크
CREATE UNIQUE INDEX IF NOT EXISTS ux_member_username ON member (username);

-- email은 값이 있을 때만 유니크(여러 NULL 허용)
CREATE UNIQUE INDEX IF NOT EXISTS ux_member_email_notnull
  ON member (email) WHERE email IS NOT NULL;

-- 소셜 계정 맵핑은 (provider, subject) 조합이 있을 때만 유니크
CREATE UNIQUE INDEX IF NOT EXISTS ux_member_oauth_notnull
  ON member (oauth_provider, oauth_subject)
  WHERE oauth_provider IS NOT NULL AND oauth_subject IS NOT NULL;

-- 4) updated_at 자동 갱신
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_member_set_updated_at ON member;
CREATE TRIGGER trg_member_set_updated_at
BEFORE UPDATE ON member
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Auth Provider table
CREATE TABLE auth_provider (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    UNIQUE (provider, provider_user_id)
);

-- Travel Group table
CREATE TABLE travel_group (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    owner_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Group Member table
CREATE TABLE group_member (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    UNIQUE (group_id, member_id)
);

-- Invitation table
CREATE TABLE invitation (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    invited_by INTEGER NOT NULL REFERENCES member(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    token VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Schedule table
CREATE TABLE schedule (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    created_by INTEGER REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Schedule Participant table
CREATE TABLE schedule_participant (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'invited',
    UNIQUE (schedule_id, member_id)
);

-- Poll table
CREATE TABLE poll (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    created_by INTEGER REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Poll Option table
CREATE TABLE poll_option (
    id SERIAL PRIMARY KEY,
    poll_id INTEGER NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_text VARCHAR(255) NOT NULL
);

-- Poll Vote table
CREATE TABLE poll_vote (
    id SERIAL PRIMARY KEY,
    poll_id INTEGER NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_id INTEGER NOT NULL REFERENCES poll_option(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    voted_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    UNIQUE (poll_id, member_id)
);

-- Expense table
CREATE TABLE expense (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    paid_by INTEGER NOT NULL REFERENCES member(id) ON DELETE SET NULL,
    paid_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Expense Share table
CREATE TABLE expense_share (
    id SERIAL PRIMARY KEY,
    expense_id INTEGER NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    share NUMERIC(12,2) NOT NULL,
    UNIQUE (expense_id, member_id)
);

-- Settlement table
CREATE TABLE settlement (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    from_member INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    to_member INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL,
    settled_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Chat Message table
CREATE TABLE chat_message (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES member(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Chat Read table
CREATE TABLE chat_read (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    read_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    UNIQUE (message_id, member_id)
);

-- Notification table
CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Guide table
CREATE TABLE guide (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    created_by INTEGER REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Guide Day table
CREATE TABLE guide_day (
    id SERIAL PRIMARY KEY,
    guide_id INTEGER NOT NULL REFERENCES guide(id) ON DELETE CASCADE,
    day_index INTEGER NOT NULL,
    date DATE,
    UNIQUE (guide_id, day_index)
);

-- Guide Item table
CREATE TABLE guide_item (
    id SERIAL PRIMARY KEY,
    guide_day_id INTEGER NOT NULL REFERENCES guide_day(id) ON DELETE CASCADE,
    time TIME,
    title VARCHAR(128) NOT NULL,
    description TEXT
);

-- Indexes for performance (examples)
CREATE INDEX idx_group_member_group ON group_member(group_id);
CREATE INDEX idx_group_member_member ON group_member(member_id);
CREATE INDEX idx_schedule_group ON schedule(group_id);
CREATE INDEX idx_expense_group ON expense(group_id);
CREATE INDEX idx_chat_message_group ON chat_message(group_id);
CREATE INDEX idx_notification_member ON notification(member_id);