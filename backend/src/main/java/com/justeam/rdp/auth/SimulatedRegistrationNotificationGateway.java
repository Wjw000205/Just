package com.justeam.rdp.auth;

import com.justeam.rdp.common.JsonSupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local acceptance adapter. It stores only masked delivery metadata in PostgreSQL; OTP values remain in process memory
 * solely so automated tests can emulate receipt and are removed after successful verification.
 */
@Component
public class SimulatedRegistrationNotificationGateway implements RegistrationNotificationGateway {
    private final JdbcClient jdbc;
    private final JsonSupport json;
    private final ConcurrentHashMap<String,VerificationCode> verificationCodes = new ConcurrentHashMap<>();

    public SimulatedRegistrationNotificationGateway(JdbcClient jdbc, JsonSupport json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override public String adapterCode() { return "SIMULATED"; }

    @Override public void deliver(Delivery delivery) {
        jdbc.sql("""
                INSERT INTO sys_notification_outbox(recipient_user_id,channel,recipient_masked,template_code,
                    reference_type,reference_id,payload_summary,adapter_code,delivery_status,delivered_time)
                VALUES (:userId,:channel,:recipient,:template,:referenceType,:referenceId,
                    CAST(:summary AS jsonb),:adapter,'DELIVERED',now())
                """).param("userId", delivery.recipientUserId())
                .param("channel", delivery.channel()).param("recipient", mask(delivery.recipient()))
                .param("template", delivery.templateCode()).param("referenceType", delivery.referenceType())
                .param("referenceId", delivery.referenceId()).param("summary", json.canonical(delivery.summary()))
                .param("adapter", adapterCode()).update();
        if (delivery.transientVerificationCode() != null && "REGISTRATION_OTP".equals(delivery.templateCode())) {
            long ttl = delivery.summary().get("expiresInSeconds") instanceof Number number ? number.longValue() : 300L;
            verificationCodes.put(delivery.referenceId(), new VerificationCode(delivery.transientVerificationCode(),
                    System.currentTimeMillis() + Math.max(60, ttl) * 1000));
        }
    }

    @Override public void forgetVerificationCode(String verificationId) {
        verificationCodes.remove(verificationId);
    }

    /** Test-only receipt simulation; no HTTP endpoint exposes this value. */
    public String peekVerificationCode(String verificationId) {
        VerificationCode value = verificationCodes.get(verificationId);
        if (value == null) return null;
        if (value.expiresAtMillis() <= System.currentTimeMillis()) { verificationCodes.remove(verificationId, value); return null; }
        return value.code();
    }

    @Scheduled(initialDelayString="${rdp.registration.simulation-cleanup-initial-delay-ms:60000}",
            fixedDelayString="${rdp.registration.simulation-cleanup-delay-ms:60000}")
    void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        verificationCodes.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return "***";
        int at = value.indexOf('@');
        if (at > 0) return value.substring(0, Math.min(2, at)) + "***" + value.substring(at);
        if (value.length() >= 7) return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        return value.charAt(0) + "***";
    }
    private record VerificationCode(String code,long expiresAtMillis) {}
}
