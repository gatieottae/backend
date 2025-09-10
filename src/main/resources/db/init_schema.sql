-- ============================================================
-- 스키마 & 검색 경로
-- ============================================================
CREATE SCHEMA IF NOT EXISTS gatieottae;
SET search_path TO gatieottae;

-- ============================================================
-- [ENUM] 회원 상태
--  - ACTIVE   : 정상 사용자
--  - BLOCKED  : 차단된 사용자
--  - DELETED  : 탈퇴/삭제된 사용자
-- ============================================================
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'member_status') THEN
            CREATE TYPE gatieottae.member_status AS ENUM ('ACTIVE','BLOCKED','DELETED');
        END IF;
    END$$;

-- ============================================================
-- 공통 updated_at 자동 갱신 트리거 함수
--  - 모든 테이블의 updated_at 컬럼을 UPDATE 시점에 now()로 업데이트
-- ============================================================
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- [회원 테이블] member
--  - 사용자 기본 정보
--  - username, email, 소셜(OAuth) 식별자에 대한 유니크 제약
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

DROP TRIGGER IF EXISTS trg_member_set_updated_at ON member;
CREATE TRIGGER trg_member_set_updated_at
    BEFORE UPDATE ON member
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- [소셜 로그인 Provider] auth_provider
--  - 외부 OAuth Provider와의 연결 정보
--  - (provider, provider_user_id) 유니크
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_provider (
                                             id                SERIAL PRIMARY KEY,
                                             member_id         BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                             provider          VARCHAR(32) NOT NULL,
                                             provider_user_id  VARCHAR(128) NOT NULL,
                                             UNIQUE (provider, provider_user_id)
);

-- ============================================================
-- [그룹 테이블] travel_group
--  - 여행 그룹의 메타 정보
--  - 그룹명은 OWNER 기준으로 유니크 (owner_id, name)
--  - 초대코드 단일 고유
-- ============================================================
CREATE TABLE IF NOT EXISTS travel_group (
                                            id           BIGSERIAL PRIMARY KEY,
                                            name         VARCHAR(30)  NOT NULL,             -- 그룹명(30자)
                                            owner_id     BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                            description  TEXT,                              -- 소개
                                            destination  VARCHAR(100),                      -- 여행지
                                            start_date   DATE,                              -- 여행 시작일
                                            end_date     DATE,                              -- 여행 종료일
                                            invite_code  VARCHAR(12) UNIQUE,                -- 단일 활성 초대코드(만료 없음)
                                            created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            UNIQUE (owner_id, name)
);

DROP TRIGGER IF EXISTS trg_travel_group_set_updated_at ON travel_group;
CREATE TRIGGER trg_travel_group_set_updated_at
    BEFORE UPDATE ON travel_group
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- [그룹 멤버 테이블] travel_group_member
--  - 그룹 가입 정보 및 역할
--  - (group_id, member_id) 유니크
--  - role: OWNER, ADMIN, MEMBER
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
-- [ENUM] 일정 참여 상태
--  - INVITED, GOING, NOT_GOING, TENTATIVE
-- ============================================================
DO $$ BEGIN
    CREATE TYPE schedule_participant_status AS ENUM ('INVITED','GOING','NOT_GOING','TENTATIVE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================
-- [ENUM] 투표 상태
--  - OPEN, CLOSED
-- ============================================================
DO $$ BEGIN
    CREATE TYPE PollStatus AS ENUM ('OPEN','CLOSED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================
-- [일정] schedule
--  - 그룹별 일정 정보 (시작/종료 시각)
--  - 종료시간 무결성: end_time > start_time (NULL 허용)
--  - 동일 그룹에서 제목+시간 완전중복 방지 (end_time NULL 대비 COALESCE)
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule (
                                        id          BIGSERIAL PRIMARY KEY,
                                        group_id    BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                        title       VARCHAR(128) NOT NULL,
                                        description TEXT,
                                        location    VARCHAR(255),
                                        start_time  TIMESTAMPTZ NOT NULL,
                                        end_time    TIMESTAMPTZ,
                                        created_by  BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                        created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        CONSTRAINT schedule_time_ck CHECK (end_time IS NULL OR end_time > start_time)
);

-- 달력/겹침 조회 최적화
CREATE INDEX IF NOT EXISTS idx_schedule_group_time
    ON schedule (group_id, start_time, end_time);

-- 동일 그룹에서 제목+시간 완전중복 방지 (end_time NULL 허용 대비)
CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_group_title_time
    ON schedule (group_id, title, start_time, COALESCE(end_time, start_time));

DROP TRIGGER IF EXISTS trg_schedule_updated_at ON schedule;
CREATE TRIGGER trg_schedule_updated_at
    BEFORE UPDATE ON schedule
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- [일정 참여자] schedule_participant
--  - 일정별 사용자 참여 상태 관리
--  - (schedule_id, member_id) 유니크
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_participant (
                                                    id           BIGSERIAL PRIMARY KEY,
                                                    schedule_id  BIGINT NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
                                                    member_id    BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                                    status       schedule_participant_status NOT NULL DEFAULT 'INVITED',
                                                    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                    UNIQUE (schedule_id, member_id)
);

-- 참석자 카운트/목록 최적화
CREATE INDEX IF NOT EXISTS idx_sp_schedule_status
    ON schedule_participant (schedule_id, status, joined_at);

-- ============================================================
-- [투표] Poll 도메인
--  - poll_category: 카테고리 정규화(ACCOM/FOOD/CAFE/ACTIVITY/TRANS/ETC)
--  - poll         : 투표 본문(OPEN 상태의 그룹+제목 부분유니크)
--  - poll_option  : 선택지(투표 내 중복 텍스트 방지)
--  - poll_vote    : 1인 1표(단일선택). 다중선택은 UNIQUE 키 변경
-- ============================================================

-- 카테고리
CREATE TABLE IF NOT EXISTS poll_category (
                                             id          BIGSERIAL PRIMARY KEY,
                                             code        VARCHAR(32)  NOT NULL UNIQUE,    -- ex) ACCOM, FOOD, CAFE, ACTIVITY, TRANS, ETC
                                             name        VARCHAR(50)  NOT NULL,           -- 표시용 이름(한글)
                                             created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                             updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS poll_category_set_updated_at ON poll_category;
CREATE TRIGGER poll_category_set_updated_at
    BEFORE UPDATE ON poll_category
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 투표 본문
CREATE TABLE IF NOT EXISTS poll (
                                    id              BIGSERIAL PRIMARY KEY,
                                    group_id        BIGINT       NOT NULL,             -- 여행 그룹 FK (그룹 테이블 존재 시 FK 연결 권장)
                                    category_id     BIGINT       NOT NULL REFERENCES poll_category(id),
                                    title           VARCHAR(200) NOT NULL,
                                    description     TEXT,
                                    status          PollStatus  NOT NULL DEFAULT 'OPEN',
                                    closes_at       TIMESTAMPTZ,                       -- 마감 시각(선택)
                                    created_by      BIGINT       NOT NULL,             -- 생성자 member_id
                                    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    -- 비즈니스 제약(선택): closes_at 이 과거 허용 여부
    -- CHECK (closes_at IS NULL OR closes_at > created_at)
);

-- 부분 인덱스: 열린(OPEN) 상태에서 그룹+제목 유니크처럼 동작
CREATE UNIQUE INDEX IF NOT EXISTS ux_poll_group_title_open
    ON poll (group_id, title)
    WHERE status = 'OPEN';

-- 조회 최적화용 인덱스
CREATE INDEX IF NOT EXISTS ix_poll_group_status ON poll (group_id, status);
CREATE INDEX IF NOT EXISTS ix_poll_closes_at   ON poll (closes_at);

DROP TRIGGER IF EXISTS poll_set_updated_at ON poll;
CREATE TRIGGER poll_set_updated_at
    BEFORE UPDATE ON poll
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 선택지
CREATE TABLE IF NOT EXISTS poll_option (
                                           id          BIGSERIAL PRIMARY KEY,
                                           poll_id     BIGINT NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
                                           content     VARCHAR(200) NOT NULL,
                                           sort_order  INT NOT NULL DEFAULT 0,           -- 선택지 정렬
                                           created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                           updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                           UNIQUE (poll_id, content)                     -- 같은 투표에서 동일 텍스트 중복 방지
);

CREATE INDEX IF NOT EXISTS ix_poll_option_poll ON poll_option (poll_id);

DROP TRIGGER IF EXISTS poll_option_set_updated_at ON poll_option;
CREATE TRIGGER poll_option_set_updated_at
    BEFORE UPDATE ON poll_option
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 투표 (단일선택) — 다중선택으로 바꾸려면 UNIQUE를 (poll_id, member_id, option_id)로
CREATE TABLE IF NOT EXISTS poll_vote (
                                         id          BIGSERIAL PRIMARY KEY,
                                         poll_id     BIGINT NOT NULL REFERENCES poll(id)        ON DELETE CASCADE,
                                         option_id   BIGINT NOT NULL REFERENCES poll_option(id) ON DELETE CASCADE,
                                         member_id   BIGINT NOT NULL,
                                         voted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         UNIQUE (poll_id, member_id)
);

-- 집계/조회 인덱스
CREATE INDEX IF NOT EXISTS ix_poll_vote_poll      ON poll_vote (poll_id);
CREATE INDEX IF NOT EXISTS ix_poll_vote_option    ON poll_vote (option_id);
CREATE INDEX IF NOT EXISTS ix_poll_vote_member    ON poll_vote (member_id);

-- 기본 카테고리 시드
INSERT INTO poll_category (code, name) VALUES
                                           ('ACCOM',   '숙소'),
                                           ('FOOD',    '음식점'),
                                           ('CAFE',    '카페'),
                                           ('ACTIVITY','활동'),
                                           ('TRANS',   '교통'),
                                           ('ETC',     '기타')
ON CONFLICT (code) DO NOTHING;

-- 참고 쿼리
-- (1) 특정 투표 결과 집계
-- SELECT o.id, o.content, COUNT(v.id) AS votes
-- FROM poll_option o
-- LEFT JOIN poll_vote v ON v.option_id = o.id
-- WHERE o.poll_id = :pollId
-- GROUP BY o.id, o.content
-- ORDER BY o.sort_order, o.id;
--
-- (2) 마감 자동 처리(Job/스케줄러):
-- UPDATE poll SET status = 'CLOSED'
-- WHERE status = 'OPEN' AND closes_at IS NOT NULL AND closes_at <= NOW();

-- ============================================================
-- [정산/비용] expense, expense_share, settlement
--  - 정산 기능 대비 스키마
-- ============================================================
CREATE TABLE IF NOT EXISTS expense (
                                       id         BIGSERIAL PRIMARY KEY,
                                       group_id   BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                       title      VARCHAR(128) NOT NULL,
                                       amount     NUMERIC(12,2) NOT NULL,
                                       paid_by    BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                       paid_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS expense_share (
                                             id          BIGSERIAL PRIMARY KEY,
                                             expense_id  BIGINT NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
                                             member_id   BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                             share       NUMERIC(12,2) NOT NULL,
                                             UNIQUE (expense_id, member_id)
);

CREATE TABLE IF NOT EXISTS settlement (
                                          id          BIGSERIAL PRIMARY KEY,
                                          group_id    BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                          from_member BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                          to_member   BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                          amount      NUMERIC(12,2) NOT NULL,
                                          settled_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- [채팅] chat_message / chat_read
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_message (
                                            id         BIGSERIAL PRIMARY KEY,
                                            group_id   BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                            sender_id  BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                            content    TEXT NOT NULL,
                                            sent_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chat_read (
                                         id          BIGSERIAL PRIMARY KEY,
                                         message_id  BIGINT NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
                                         member_id   BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                         read_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         UNIQUE (message_id, member_id)
);

-- ============================================================
-- [알림] notification
-- ============================================================
CREATE TABLE IF NOT EXISTS notification (
                                            id         BIGSERIAL PRIMARY KEY,
                                            member_id  BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
                                            type       VARCHAR(32) NOT NULL,
                                            content    TEXT NOT NULL,
                                            is_read    BOOLEAN DEFAULT FALSE,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- [여행 가이드] guide / guide_day / guide_item
-- ============================================================
CREATE TABLE IF NOT EXISTS guide (
                                     id           BIGSERIAL PRIMARY KEY,
                                     group_id     BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
                                     title        VARCHAR(128) NOT NULL,
                                     description  TEXT,
                                     created_by   BIGINT REFERENCES member(id) ON DELETE SET NULL,
                                     created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS trg_guide_updated_at ON guide;
CREATE TRIGGER trg_guide_updated_at
    BEFORE UPDATE ON guide
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS guide_day (
                                         id        BIGSERIAL PRIMARY KEY,
                                         guide_id  BIGINT NOT NULL REFERENCES guide(id) ON DELETE CASCADE,
                                         day_index INTEGER NOT NULL,
                                         date      DATE,
                                         UNIQUE (guide_id, day_index)
);

CREATE TABLE IF NOT EXISTS guide_item (
                                          id           BIGSERIAL PRIMARY KEY,
                                          guide_day_id BIGINT NOT NULL REFERENCES guide_day(id) ON DELETE CASCADE,
                                          time         TIME,
                                          title        VARCHAR(128) NOT NULL,
                                          description  TEXT
);

-- ============================================================
-- [인덱스 모음]
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_travel_group_member_group ON travel_group_member(group_id);
CREATE INDEX IF NOT EXISTS idx_travel_group_member_member ON travel_group_member(member_id);
CREATE INDEX IF NOT EXISTS idx_schedule_group ON schedule(group_id);
CREATE INDEX IF NOT EXISTS idx_expense_group ON expense(group_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_group ON chat_message(group_id);
CREATE INDEX IF NOT EXISTS idx_notification_member ON notification(member_id);