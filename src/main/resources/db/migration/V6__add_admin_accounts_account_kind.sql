ALTER TABLE admin_accounts
    ADD COLUMN account_kind VARCHAR(50) NOT NULL DEFAULT 'GOVERNMENT';

UPDATE admin_accounts
SET account_kind = 'CONTRACTOR'
WHERE email NOT LIKE '%.go.kr'
  AND email NOT LIKE '%@korea.kr'
  AND email NOT LIKE '%@go.kr';

ALTER TABLE admin_accounts
    ALTER COLUMN job_rank DROP NOT NULL;
