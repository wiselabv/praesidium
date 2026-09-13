package io.github.wiselabv.praesidium.admin.identity.application;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * TOTP 服务（RFC 6238）：两步验证动态码的生成与校验。
 *
 * <p>基于 JDK 内置密码学实现（HmacSHA1 + 动态截断），不引入第三方库；
 * Base32 编解码按 RFC 4648 手写（JDK 只提供 Base64）。
 * 时间步长 30 秒、码长 6 位，校验窗口 ±1 步（容忍轻微时钟偏移）。
 */
@Service
public class TotpService {

    /** RFC 4648 Base32 字母表 */
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int SECRET_BYTES = 20;
    private static final int VERIFY_WINDOW = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    /** 生成新的 TOTP 密钥（Base32 编码，20 字节熵） */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /** 校验动态码：时间步窗口 ±1，常数时间比较防时序侧信道 */
    public boolean verify(String secretBase32, String code) {
        if (secretBase32 == null || code == null || !code.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }
        byte[] secret = decodeBase32(secretBase32);
        byte[] given = code.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        long step = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        for (int offset = -VERIFY_WINDOW; offset <= VERIFY_WINDOW; offset++) {
            int expected = hotp(secret, step + offset);
            if (MessageDigest.isEqual(
                    String.format("%06d", expected).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    given)) {
                return true;
            }
        }
        return false;
    }

    /** 生成 otpauth URI（供前端渲染二维码导入 TOTP App） */
    public String otpAuthUri(String secretBase32, String account, String issuer) {
        return "otpauth://totp/" + issuer + ":" + account
                + "?secret=" + secretBase32
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    /** HOTP：HMAC-SHA1 动态截断（RFC 4226） */
    private int hotp(byte[] secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return binary % 1_000_000;
        } catch (Exception ex) {
            throw new IllegalStateException("HmacSHA1 不可用", ex);
        }
    }

    /** RFC 4648 Base32 编码（无填充） */
    private static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1F]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
        }
        return sb.toString();
    }

    /** RFC 4648 Base32 解码（容忍大小写与填充） */
    private static byte[] decodeBase32(String input) {
        String normalized = input.toUpperCase().replace("=", "");
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : normalized.toCharArray()) {
            int value = indexOf(c);
            if (value < 0) {
                continue;
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    private static int indexOf(char c) {
        for (int i = 0; i < BASE32_ALPHABET.length; i++) {
            if (BASE32_ALPHABET[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
