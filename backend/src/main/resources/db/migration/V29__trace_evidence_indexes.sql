-- TRACE-003: make entity-scoped audit evidence lookup stable at production volume.
CREATE INDEX idx_audit_trace_entity_evidence
    ON sys_audit_log ((details->>'entityId'), created_time DESC, id DESC)
    WHERE module = 'TRACE';

CREATE INDEX idx_audit_trace_from_evidence
    ON sys_audit_log ((details->>'from'), created_time DESC, id DESC)
    WHERE module = 'TRACE';

CREATE INDEX idx_audit_trace_to_evidence
    ON sys_audit_log ((details->>'to'), created_time DESC, id DESC)
    WHERE module = 'TRACE';

CREATE INDEX idx_audit_file_business_evidence
    ON sys_audit_log ((details->>'businessType'), (details->>'businessRef'), created_time DESC, id DESC)
    WHERE module = 'FILE';
