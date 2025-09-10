-- ============================================================
-- Schema search_path
-- ============================================================
CREATE SCHEMA IF NOT EXISTS gatieottae;
SET search_path TO gatieottae;

-- ============================================================
-- 1) chat_message 보강
--    - SYSTEM 메시지 구분, 멘션, 소프트 삭제 지원
-- ============================================================
ALTER TABLE IF EXISTS chat_message
    ADD COLUMN IF NOT EXISTS type        VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS mentions    JSONB,                 -- [1,2,3] 형태(member_id 배열) 또는 {ids:[...]}
    ADD COLUMN IF NOT EXISTS deleted_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by  BIGINT REFERENCES member(id) ON DELETE SET NULL;

-- 메시지 조회 최적화(그룹 + 시간 역순 스크롤)
CREATE INDEX IF NOT EXISTS idx_chat_message_group_sent_at
    ON chat_message (group_id, sent_at DESC);

-- 발신자 조회 보조(선택)
CREATE INDEX IF NOT EXISTS idx_chat_message_sender
    ON chat_message (sender_id);

-- ============================================================
-- 2) chat_read 인덱스 보강 (읽음 동기화·배지 계산 가속)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_chat_read_member_message
    ON chat_read (member_id, message_id);

-- 마지막 읽은 메시지 빠른 탐색(선택)
CREATE INDEX IF NOT EXISTS idx_chat_read_message_member
    ON chat_read (message_id, member_id);

-- ============================================================
-- 3) notification 보강
--    - 그룹 단위 배지/필터링, 유연한 payload 저장
-- ============================================================
ALTER TABLE IF EXISTS notification
    ADD COLUMN IF NOT EXISTS group_id BIGINT REFERENCES travel_group(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS payload  JSONB;

-- 알림 목록/배지 최적화(미확인 우선 + 최신순)
CREATE INDEX IF NOT EXISTS idx_notification_member_isread_created
    ON notification (member_id, is_read, created_at DESC);

-- 그룹별 알림 조회 보조(선택)
CREATE INDEX IF NOT EXISTS idx_notification_group_member_created
    ON notification (group_id, member_id, created_at DESC);

-- ============================================================
-- 4) 그룹별 알림 설정(뮤트) 테이블
--    - 뮤트 on/off 및 기간 지정
-- ============================================================
CREATE TABLE IF NOT EXISTS group_notification_setting (
                                                          member_id  BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    group_id   BIGINT NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    muted      BOOLEAN NOT NULL DEFAULT FALSE,
    mute_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (member_id, group_id)
    );

-- 조회 최적화
CREATE INDEX IF NOT EXISTS idx_group_notiset_group
    ON group_notification_setting (group_id);
CREATE INDEX IF NOT EXISTS idx_group_notiset_member
    ON group_notification_setting (member_id);

-- ============================================================
-- 5) 유지보수용 코멘트 (문서화)
-- ============================================================
COMMENT ON COLUMN chat_message.type
  IS '메시지 타입: NORMAL|SYSTEM';
COMMENT ON COLUMN chat_message.mentions
  IS '멘션 대상 member_id 목록(JSONB)';
COMMENT ON TABLE group_notification_setting
  IS '그룹별 알림 설정(뮤트/기간)';