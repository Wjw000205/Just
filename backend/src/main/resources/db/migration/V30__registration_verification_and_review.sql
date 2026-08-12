CREATE TABLE sys_registration_application (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES sys_user(id),
    verification_channel VARCHAR(10) NOT NULL CHECK (verification_channel IN ('EMAIL','PHONE')),
    destination_masked VARCHAR(120) NOT NULL,
    destination_digest CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    submitted_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES sys_user(id),
    reviewed_time TIMESTAMPTZ,
    review_comment VARCHAR(1000),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_registration_application_status_time
    ON sys_registration_application(status, submitted_time DESC, id DESC);

CREATE UNIQUE INDEX uk_sys_user_username_normalized
    ON sys_user(lower(username)) WHERE deleted=0;
CREATE UNIQUE INDEX uk_sys_user_email_normalized
    ON sys_user(lower(email)) WHERE deleted=0 AND email IS NOT NULL;

CREATE TABLE sys_notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT REFERENCES sys_user(id),
    channel VARCHAR(16) NOT NULL CHECK (channel IN ('EMAIL','PHONE','SYSTEM')),
    recipient_masked VARCHAR(120) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id VARCHAR(120),
    payload_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    adapter_code VARCHAR(32) NOT NULL,
    delivery_status VARCHAR(16) NOT NULL CHECK (delivery_status IN ('DELIVERED','FAILED')),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_time TIMESTAMPTZ,
    error_message VARCHAR(500)
);

CREATE INDEX idx_notification_outbox_reference
    ON sys_notification_outbox(reference_type, reference_id, created_time DESC);
CREATE INDEX idx_notification_outbox_recipient
    ON sys_notification_outbox(recipient_user_id, created_time DESC)
    WHERE recipient_user_id IS NOT NULL;
