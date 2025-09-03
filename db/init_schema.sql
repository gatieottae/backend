CREATE SCHEMA IF NOT EXISTS gatieottae;
SET search_path TO gatieottae;

-- ============================================================
-- [ENUM] 회원 상태
-- ACTIVE: 정상, BLOCKED: 차단, DELETED: 탈퇴/삭제
-- ============================================================
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'member_status') THEN
            CREATE TYPE gatieottae.member_status AS ENUM ('ACTIVE','BLOCKED','DELETED');
        END IF;
    END$$;

-- ============================================================
-- [회원 테이블] member
-- ============================================================
CREATE TABLE IF NOT EXISTS member (
                                      id                BIGSERIAL PRIMARY KEY,
                                      username          VARCHAR(50)  NOT NULL,
                                      password_hash     VARCHAR(255) NOT NULL,
                                      email             VARCHAR(255),
                                      name              VARCHAR(50)  NOT NULL,
                                      nickname          VARCHAR(50),
                                      profile_image_url TEXT,
                                      status            member_status NOT NULL DEFAULT 'ACTIVE',
                                      oauth_provider    VARCHAR(20),
                                      oauth_subject     VARCHAR(100),
                                      last_login_at     TIMESTAMPTZ,
                                      created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_member_username ON member (username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_member_email_notnull
    ON member (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_member_oauth_notnull
    ON member (oauth_provider, oauth_subject)
    WHERE oauth_provider IS NOT NULL AND oauth_subject IS NOT NULL;

-- updated_at 트리거
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

-- ============================================================
-- [소셜 로그인 Provider]
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_provider (
                                             id SERIAL PRIMARY KEY,
                                             member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                             provider VARCHAR(32) NOT NULL,
                                             provider_user_id VARCHAR(128) NOT NULL,
                                             UNIQUE (provider, provider_user_id)
);

-- ============================================================
-- [그룹 테이블] travel_group
-- ============================================================
CREATE TABLE IF NOT EXISTS travel_group (
                                            id                BIGSERIAL PRIMARY KEY,
                                            name              VARCHAR(128) NOT NULL,
                                            description       TEXT,
                                            owner_id          BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                            invite_code       VARCHAR(12) UNIQUE,
                                            invite_expires_at TIMESTAMPTZ,
                                            invite_rotated_at TIMESTAMPTZ,
                                            created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            UNIQUE (owner_id, name)
);

DROP TRIGGER IF EXISTS trg_travel_group_set_updated_at ON travel_group;
CREATE TRIGGER trg_travel_group_set_updated_at
    BEFORE UPDATE ON travel_group
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- [그룹 멤버 테이블] travel_group_member
-- ============================================================
CREATE TABLE IF NOT EXISTS travel_group_member (
                                                   id        BIGSERIAL PRIMARY KEY,
                                                   group_id  BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                                   member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                                   role      VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
                                                   joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                   UNIQUE (group_id, member_id),
                                                   CONSTRAINT chk_travel_group_member_role CHECK (role IN ('OWNER','ADMIN','MEMBER'))
);

DROP TRIGGER IF EXISTS trg_travel_group_member_set_updated_at ON travel_group_member;
CREATE TRIGGER trg_travel_group_member_set_updated_at
    BEFORE UPDATE ON travel_group_member
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- [일정 테이블] schedule
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule (
                                        id BIGSERIAL PRIMARY KEY,
                                        group_id   BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                        title      VARCHAR(128) NOT NULL,
                                        description TEXT,
                                        location   VARCHAR(255),
                                        start_time TIMESTAMPTZ NOT NULL,
                                        end_time   TIMESTAMPTZ,
                                        created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 참가자
CREATE TABLE IF NOT EXISTS schedule_participant (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    schedule_id BIGINT NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
                                                    member_id   BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                                    status      VARCHAR(32) NOT NULL DEFAULT 'invited',
                                                    UNIQUE (schedule_id, member_id)
);

-- ============================================================
-- [투표 테이블] poll
-- ============================================================
CREATE TABLE IF NOT EXISTS poll (
                                    id BIGSERIAL PRIMARY KEY,
                                    group_id   BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                    title      VARCHAR(128) NOT NULL,
                                    description TEXT,
                                    created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    expires_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS poll_option (
                                           id BIGSERIAL PRIMARY KEY,
                                           poll_id BIGINT NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
                                           option_text VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS poll_vote (
                                         id BIGSERIAL PRIMARY KEY,
                                         poll_id   BIGINT NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
                                         option_id BIGINT NOT NULL REFERENCES poll_option(id) ON DELETE CASCADE,
                                         member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                         voted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         UNIQUE (poll_id, member_id)
);

-- ============================================================
-- [정산/비용]
-- ============================================================
CREATE TABLE IF NOT EXISTS expense (
                                       id BIGSERIAL PRIMARY KEY,
                                       group_id  BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                       title     VARCHAR(128) NOT NULL,
                                       amount    NUMERIC(12,2) NOT NULL,
                                       paid_by   BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                       paid_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS expense_share (
                                             id BIGSERIAL PRIMARY KEY,
                                             expense_id BIGINT NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
                                             member_id  BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                             share NUMERIC(12,2) NOT NULL,
                                             UNIQUE (expense_id, member_id)
);

CREATE TABLE IF NOT EXISTS settlement (
                                          id BIGSERIAL PRIMARY KEY,
                                          group_id    BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                          from_member BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                          to_member   BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                          amount NUMERIC(12,2) NOT NULL,
                                          settled_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- [채팅]
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_message (
                                            id BIGSERIAL PRIMARY KEY,
                                            group_id  BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                            sender_id BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                            content   TEXT NOT NULL,
                                            sent_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chat_read (
                                         id BIGSERIAL PRIMARY KEY,
                                         message_id BIGINT NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
                                         member_id  BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                         read_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         UNIQUE (message_id, member_id)
);

-- ============================================================
-- [알림] notification
-- ============================================================
CREATE TABLE IF NOT EXISTS notification (
                                            id BIGSERIAL PRIMARY KEY,
                                            member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                            type VARCHAR(32) NOT NULL,
                                            content TEXT NOT NULL,
                                            is_read BOOLEAN DEFAULT FALSE,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- [여행 가이드]
-- ============================================================
CREATE TABLE IF NOT EXISTS guide (
                                     id BIGSERIAL PRIMARY KEY,
                                     group_id   BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                     title      VARCHAR(128) NOT NULL,
                                     description TEXT,
                                     created_by BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS guide_day (
                                         id BIGSERIAL PRIMARY KEY,
                                         guide_id BIGINT NOT NULL REFERENCES guide(id) ON DELETE CASCADE,
                                         day_index INTEGER NOT NULL,
                                         date DATE,
                                         UNIQUE (guide_id, day_index)
);

CREATE TABLE IF NOT EXISTS guide_item (
                                          id BIGSERIAL PRIMARY KEY,
                                          guide_day_id BIGINT NOT NULL REFERENCES guide_day(id) ON DELETE CASCADE,
                                          time TIME,
                                          title VARCHAR(128) NOT NULL,
                                          description TEXT
);

-- ============================================================
-- [인덱스]
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_travel_group_member_group ON travel_group_member(group_id);
CREATE INDEX IF NOT EXISTS idx_travel_group_member_member ON travel_group_member(member_id);
CREATE INDEX IF NOT EXISTS idx_schedule_group ON schedule(group_id);
CREATE INDEX IF NOT EXISTS idx_expense_group ON expense(group_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_group ON chat_message(group_id);
CREATE INDEX IF NOT EXISTS idx_notification_member ON notification(member_id);