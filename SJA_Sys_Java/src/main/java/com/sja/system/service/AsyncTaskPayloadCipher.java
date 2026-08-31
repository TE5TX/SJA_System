package com.sja.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 对 Redis 中的任务正文进行带完整性校验的加密。
 */
@Slf4j
@Component
public class AsyncTaskPayloadCipher {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public AsyncTaskPayloadCipher(
            @Value("${sja.async-task.encryption-key:}") String encryptionKey,
            @Value("${jwt.secret}") String jwtSecret) {
        String sourceKey = StringUtils.hasText(encryptionKey) ? encryptionKey : jwtSecret;
        if (!StringUtils.hasText(encryptionKey)) {
            log.warn("SJA_ASYNC_TASK_ENCRYPTION_KEY 未配置，异步队列暂时复用 JWT 密钥；生产环境应配置独立密钥");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sourceKey.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化异步任务加密器失败", e);
        }
    }

    public String encrypt(String plaintext, String context) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array());
        } catch (Exception e) {
            throw new IllegalStateException("异步任务正文加密失败", e);
        }
    }

    public String decrypt(String ciphertext, String context) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不正确");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("异步任务正文解密或完整性校验失败", e);
        }
    }

    /**
     * 生成只允许读取单个任务状态的确定性令牌，Redis 无需保存令牌明文。
     */
    public String deriveStatusToken(String taskId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            byte[] digest = mac.doFinal(("status|" + taskId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("生成异步任务状态令牌失败", e);
        }
    }
}
