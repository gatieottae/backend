-- 0) 스키마 고정 (해당 마이그레이션 트랜잭션 범위에서만)
SET LOCAL search_path TO gatieottae, public;

-- 1) 컬럼 추가 (의존 없는 순수 스키마 변경)
ALTER TABLE gatieottae.transfer
    ADD COLUMN IF NOT EXISTS expense_id BIGINT;

-- 2) 타임스탬프 기본값(now) 보장 + 과거 데이터 보정
ALTER TABLE gatieottae.transfer
    ALTER COLUMN created_at SET DEFAULT now(),
    ALTER COLUMN updated_at SET DEFAULT now();

UPDATE gatieottae.transfer SET created_at = now() WHERE created_at IS NULL;
UPDATE gatieottae.transfer SET updated_at = now() WHERE updated_at IS NULL;

-- 3) 타임스탬프 트리거 함수 (INSERT/UPDATE에 updated_at 갱신, INSERT시 created_at 보장)
CREATE OR REPLACE FUNCTION gatieottae.set_transfer_timestamps()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.created_at IS NULL THEN
            NEW.created_at := now();
        END IF;
    END IF;

    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

-- 4) 트리거 재생성 (있으면 제거 후 재생성)
DROP TRIGGER IF EXISTS trg_transfer_set_timestamps ON gatieottae.transfer;

CREATE TRIGGER trg_transfer_set_timestamps
    BEFORE INSERT OR UPDATE ON gatieottae.transfer
    FOR EACH ROW EXECUTE FUNCTION gatieottae.set_transfer_timestamps();

-- 5) 조회 성능용 인덱스 (새 컬럼에 대한 인덱스는 마지막에)
CREATE INDEX IF NOT EXISTS idx_transfer_expense
    ON gatieottae.transfer(expense_id);