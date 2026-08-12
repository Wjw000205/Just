ALTER TABLE electronic_signature
    ADD COLUMN signature_key_id VARCHAR(100) NOT NULL DEFAULT 'legacy-unversioned';

COMMENT ON COLUMN electronic_signature.signature_key_id IS
    'HMAC key version used for signature verification; legacy rows require the matching key in RDP_AUDIT_PREVIOUS_KEYS';

CREATE INDEX idx_signature_key_id ON electronic_signature(signature_key_id);
