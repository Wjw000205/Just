package com.justeam.rdp.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCipher {
    private static final String PREFIX = "v1.";
    private static final int NONCE_BYTES = 12;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(RdpProperties properties) {
        String configured = properties.security().dataEncryptionKey();
        if (configured == null || configured.length() < 32) {
            throw new IllegalStateException("RDP_DATA_ENCRYPTION_KEY至少需要32个字符");
        }
        try {
            this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                    .digest(configured.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] nonce = new byte[NONCE_BYTES]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (Exception ex) {
            throw new IllegalStateException("敏感配置加密失败", ex);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        if (!ciphertext.startsWith(PREFIX)) throw new IllegalStateException("不支持的密文版本");
        try {
            byte[] value = Base64.getUrlDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (value.length <= NONCE_BYTES) throw new IllegalArgumentException("密文长度无效");
            byte[] nonce = java.util.Arrays.copyOfRange(value, 0, NONCE_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(value, NONCE_BYTES, value.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("敏感配置解密失败", ex);
        }
    }
}
