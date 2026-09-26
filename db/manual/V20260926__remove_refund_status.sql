-- Remove the refund feature from the payments table.
--
-- PaymentStatus no longer declares REFUNDED. Because the enum is mapped with
-- @Enumerated(EnumType.STRING), any row still holding that value cannot be hydrated
-- into the Payment entity and will throw "No enum constant" on read. Run this BEFORE
-- deploying the code change.
--
-- Rows are moved to FAILED rather than SUCCESSFUL on purpose: refunded orders were
-- already excluded from revenue (revenue only counted SUCCESSFUL), so FAILED keeps
-- historical revenue and analytics figures unchanged. Mapping them to SUCCESSFUL
-- would retroactively inflate revenue.

UPDATE tb_payments
SET status = 'FAILED',
    error_message = COALESCE(NULLIF(error_message, ''), 'refund support unavailable')
WHERE status IN ('REFUNDED', 'REFUND_PENDING');

-- Narrow the column type to match the Java enum, so the database rejects a
-- reintroduced 'REFUNDED' instead of silently storing a value the app cannot
-- hydrate. This MUST stay after the UPDATE above: MySQL converts out-of-range
-- ENUM members to '' (or errors under strict mode) rather than failing safely.
ALTER TABLE tb_payments
    MODIFY status ENUM('FAILED', 'PENDING', 'SUCCESSFUL') NOT NULL;

-- Verify no refund statuses remain before deploying.
SELECT status, COUNT(*) AS total
FROM tb_payments
GROUP BY status;

-- ---------------------------------------------------------------------------
-- Rollback (only if the refund feature is ever restored)
-- ---------------------------------------------------------------------------
-- Restoring these rows requires re-adding 'REFUNDED' to BOTH the Java enum and
-- the column type; the ALTER above will reject a write otherwise. Reverting the
-- data alone leaves the app crashing on read.
--
-- Before-image of the rows this migration rewrote on the dev database
-- (db_Aiperfume, 2026-09-26). Columns not listed here were already correct.
--
--   id     | amount | created_at             | paid_at                | method | txn                            | order | error_message
--   11005  | 770.00 | 2026-09-05 13:05:00.000 | 2026-09-05 14:05:00.000 | ABA    | TXN-11005                      | 905   | NULL
--   11010  | 820.00 | 2026-09-05 13:10:00.000 | 2026-09-05 14:10:00.000 | ACLEDA | TXN-11010                      | 910   | NULL
--   11015  | 870.00 | 2026-09-05 13:15:00.000 | 2026-09-05 14:15:00.000 | CASH   | TXN-11015                      | 915   | NULL
--   11064  | 100.00 | 2026-09-26 17:09:25.875 | 2026-09-26 17:10:19.950 | KHQR   | e20da40c-8a34-4774-ae73-a00b34746d40 | 970 | sad
--   11065  | 100.00 | 2026-09-26 17:18:07.323 | 2026-09-26 17:18:37.313 | KHQR   | 88379f23-2c30-4f90-9578-f5943b75fd6c | 971 | they don't like the smell
--
-- All five had status = 'REFUNDED' before this migration. The three ABA/ACLEDA/CASH
-- rows (11005-11015) are the db/seed_test_data.sql fixtures; 11064 and 11065 are
-- real dev transactions against orders 970 and 971.
-- ---------------------------------------------------------------------------
