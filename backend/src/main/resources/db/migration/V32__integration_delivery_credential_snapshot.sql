ALTER TABLE integration_job
    ADD COLUMN IF NOT EXISTS delivery_secret_ciphertext TEXT;

-- V31 jobs used the integration's then-current HMAC secret. Preserve that
-- credential as an immutable retry snapshot before current credentials rotate.
UPDATE integration_job j
SET delivery_secret_ciphertext = c.secret_ciphertext
FROM integration_config c
WHERE j.integration_id = c.id
  AND upper(COALESCE(j.processing_config->>'deliveryMode','NONE')) = 'HTTP'
  AND (
      (j.local_committed = TRUE AND j.delivery_status = 'PENDING')
      OR j.status IN ('RECEIVED','PROCESSING','FAILED','MANUAL_REVIEW')
  )
  AND j.delivery_secret_ciphertext IS NULL;

COMMENT ON COLUMN integration_job.delivery_secret_ciphertext IS
    'Encrypted immutable outbound HMAC credential snapshot for this accepted job; never returned by APIs';
