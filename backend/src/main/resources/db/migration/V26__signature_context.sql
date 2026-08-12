ALTER TABLE electronic_signature
    ADD COLUMN signature_ip VARCHAR(64) NOT NULL DEFAULT 'legacy-unknown';

COMMENT ON COLUMN electronic_signature.signature_ip IS
    'Remote address observed by the trusted application boundary when the signature was created';
