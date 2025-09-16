-- ============================================================
-- Flyway Migration: Update expense & settlement schema
-- ============================================================

-- 1. expense.amount / expense_share.share 컬럼 타입 변경 (NUMERIC → BIGINT)
ALTER TABLE gatieottae.expense
ALTER COLUMN amount TYPE BIGINT USING ROUND(amount);

ALTER TABLE gatieottae.expense_share
ALTER COLUMN share TYPE BIGINT USING ROUND(share);

-- 2. settlement → transfer 테이블로 리네임
ALTER TABLE gatieottae.settlement RENAME TO transfer;

-- 3. transfer 테이블 컬럼 구조 확장
ALTER TABLE gatieottae.transfer
    RENAME COLUMN from_member TO from_member_id;

ALTER TABLE gatieottae.transfer
    RENAME COLUMN to_member TO to_member_id;

-- 기존 settled_at → created_at 으로 리네임
ALTER TABLE gatieottae.transfer
    RENAME COLUMN settled_at TO created_at;

-- 상태 컬럼 추가 (REQUESTED, SENT, CONFIRMED, ROLLED_BACK)
DO $$ BEGIN
CREATE TYPE gatieottae.transfer_status AS ENUM ('REQUESTED','SENT','CONFIRMED','ROLLED_BACK');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE gatieottae.transfer
    ADD COLUMN status transfer_status NOT NULL DEFAULT 'REQUESTED';

-- 증빙 URL / 메모 추가
ALTER TABLE gatieottae.transfer
    ADD COLUMN proof_url TEXT,
    ADD COLUMN memo TEXT;

-- updated_at 추가
ALTER TABLE gatieottae.transfer
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 4. 트리거: updated_at 자동 갱신
DROP TRIGGER IF EXISTS trg_expense_set_updated_at ON gatieottae.expense;
CREATE TRIGGER trg_expense_set_updated_at
    BEFORE UPDATE ON gatieottae.expense
    FOR EACH ROW EXECUTE FUNCTION gatieottae.set_updated_at();

DROP TRIGGER IF EXISTS trg_expense_share_set_updated_at ON gatieottae.expense_share;
CREATE TRIGGER trg_expense_share_set_updated_at
    BEFORE UPDATE ON gatieottae.expense_share
    FOR EACH ROW EXECUTE FUNCTION gatieottae.set_updated_at();

DROP TRIGGER IF EXISTS trg_transfer_set_updated_at ON gatieottae.transfer;
CREATE TRIGGER trg_transfer_set_updated_at
    BEFORE UPDATE ON gatieottae.transfer
    FOR EACH ROW EXECUTE FUNCTION gatieottae.set_updated_at();

-- ============================================================
-- 인덱스 최적화
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_transfer_group ON gatieottae.transfer(group_id);
CREATE INDEX IF NOT EXISTS idx_transfer_from_to ON gatieottae.transfer(from_member_id, to_member_id);