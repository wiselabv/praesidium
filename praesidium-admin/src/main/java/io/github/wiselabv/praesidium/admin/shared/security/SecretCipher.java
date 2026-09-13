package io.github.wiselabv.praesidium.admin.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 敏感内容加解密（AES-256-GCM）：凭据私钥 / 密码、SSO client secret 等落库前加密。
 *
 * <p>密钥经 {@code PRAESIDIUM_SECRET_KEY} 环境变量注入（≥32 字符），
 * 默认值仅限本地开发。密文格式：{@code enc:v1:<iv base64>:<cipher base64>}。
 */
@Component
public class SecretCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String PREFIX = "enc:v1:";

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretCipher(@Value("${praesidium.security.secret-key}") String secretKey) {
        this.keySpec = new SecretKeySpec(sha256(secretKey), "AES");
    }

    /** 加密为可落库字符串 */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new IllegalStateException("敏感内容加密失败", ex);
        }
    }

    /** 解密；格式非法或篡改返回 null（调用方按「不存在」处理） */
    public String decrypt(String encrypted) {
        if (encrypted == null || !encrypted.startsWith(PREFIX)) {
            return null;
        }
        try {
            String[] parts = encrypted.substring(PREFIX.length()).split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec,
                    new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    /** 脱敏展示：保留头部 4 字符，其余以 … 替代 */
    public String mask(String plain) {
        if (plain == null || plain.isBlank()) {
            return "";
        }
        String head = plain.length() > 8 ? plain.substring(0, 8) : plain.substring(0, Math.min(4, plain.length()));
        return head + "…";
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
