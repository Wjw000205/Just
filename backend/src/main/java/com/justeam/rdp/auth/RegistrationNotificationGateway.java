package com.justeam.rdp.auth;

import java.util.Map;

/**
 * Replaceable delivery boundary for registration notifications. Implementations must never persist or log OTP values.
 */
public interface RegistrationNotificationGateway {
    String adapterCode();
    void deliver(Delivery delivery);
    default void forgetVerificationCode(String verificationId) {}

    record Delivery(Long recipientUserId, String channel, String recipient, String templateCode,
                    String referenceType, String referenceId, Map<String,Object> summary,
                    String transientVerificationCode) {}
}
