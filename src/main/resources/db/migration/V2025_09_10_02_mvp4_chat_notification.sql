-- 읽음 중복 방지 — chat_read에 유니크 키
ALTER TABLE IF EXISTS gatieottae.chat_read
    ADD CONSTRAINT uq_chat_read_message_member UNIQUE (message_id, member_id);

-- 메시지 조회 최적화 — “삭제 안 된 것만” 부분 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_message_group_sent_at_active
    ON gatieottae.chat_message (group_id, sent_at DESC)
    WHERE deleted_at IS NULL;

-- 멘션 검색 많이 쓰면 — JSONB GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_message_mentions_gin
    ON gatieottae.chat_message USING gin (mentions jsonb_path_ops);