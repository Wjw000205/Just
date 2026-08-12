ALTER TABLE sys_user ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_user ADD COLUMN bootstrap_pending BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE sys_user SET status=0,
 password='$2y$10$disabled.bootstrap.account.hash.not.usable0000000000000000000',
 must_change_password=TRUE, bootstrap_pending=TRUE
WHERE username='admin' AND password='$2y$10$V1.zUhQBC9kMcv3RHl57s.K6GBLGyoDxhjWX0HwnTVvmKH99TLgX6';
