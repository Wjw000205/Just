-- V32 introduced immutable delivery credential snapshots. V33 preserves the
-- published V32 checksum and adds a fail-closed Java validation hand-off for
-- credentials created before the 32-byte HMAC policy was enforced.
--
-- SQL cannot inspect AES-GCM plaintext strength. Previously-active HMAC
-- integrations remain disabled until IntegrationCredentialUpgradeInitializer
-- decrypts and validates them during startup. Strong keys are restored
-- automatically; weak or unreadable keys require an explicit rotation.
UPDATE integration_job
SET idempotency_key = 'legacy-job-' || id
WHERE idempotency_key IS NULL;

ALTER TABLE integration_job
    ALTER COLUMN idempotency_key SET NOT NULL;

UPDATE integration_config
SET active = FALSE,
    config = config || '{"credentialValidationRequired":true}'::jsonb,
    version = version + 1,
    updated_time = now()
WHERE auth_type = 'HMAC'
  AND active = TRUE;
