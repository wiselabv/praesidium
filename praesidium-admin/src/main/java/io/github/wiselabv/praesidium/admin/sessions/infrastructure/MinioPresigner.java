package io.github.wiselabv.praesidium.admin.sessions.infrastructure;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MinIO 录像对象预签名 URL 生成器。
 *
 * <p>纯 JDK 实现 AWS SigV4 查询参数签名（path-style），避免为录像下载引入 AWS SDK。
 * 前端拿预签名 URL 直连 MinIO 拉切片回放/下载，Java 服务本身不代理对象流量。
 */
@Component
public class MinioPresigner {

    private static final String REGION = "us-east-1";
    private static final String SERVICE = "s3";
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;

    public MinioPresigner(@Value("${praesidium.minio.endpoint:http://192.168.10.4:9000}") String endpoint,
                          @Value("${praesidium.minio.access-key:minio}") String accessKey,
                          // 密钥为真实凭据，经环境变量注入，不落代码默认值（与 application.yaml 同源）
                          @Value("${praesidium.minio.secret-key:}") String secretKey,
                          @Value("${praesidium.minio.bucket:praesidium-recordings}") String bucket) {
        this.endpoint = endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
    }

    /** 生成对象 GET 预签名 URL（默认 10 分钟过期） */
    public String presignGet(String objectKey) {
        return presignGet(objectKey, 600);
    }

    /** 生成对象 GET 预签名 URL，expiresSeconds 秒后过期 */
    public String presignGet(String objectKey, long expiresSeconds) {
        String amzDate = AMZ_DATE.format(Instant.now());
        String dateStamp = amzDate.substring(0, 8);
        String credentialScope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";

        String canonicalUri = "/" + bucket + "/" + encodePath(objectKey);
        String canonicalQuery = "X-Amz-Algorithm=AWS4-HMAC-SHA256"
                + "&X-Amz-Credential=" + urlEncode(accessKey) + "%2F" + dateStamp
                + "%2F" + REGION + "%2F" + SERVICE + "%2Faws4_request"
                + "&X-Amz-Date=" + amzDate
                + "&X-Amz-Expires=" + expiresSeconds
                + "&X-Amz-SignedHeaders=host";
        String host = host();
        String canonicalRequest = "GET\n" + canonicalUri + "\n" + canonicalQuery
                + "\nhost:" + host + "\n\nhost\nUNSIGNED-PAYLOAD";

        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope
                + "\n" + sha256Hex(canonicalRequest);
        byte[] signingKey = hmac(hmac(hmac(hmac(
                ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp),
                REGION), SERVICE), "aws4_request");
        String signature = hex(hmac(signingKey, stringToSign));

        return endpoint + canonicalUri + "?" + canonicalQuery + "&X-Amz-Signature=" + signature;
    }

    /** 端点主机（含端口），用于 Host 头签名 */
    private String host() {
        String noScheme = endpoint.replaceFirst("^https?://", "");
        int slash = noScheme.indexOf('/');
        return slash < 0 ? noScheme : noScheme.substring(0, slash);
    }

    /** 对象 key 按路径段编码（保留 /，段内 RFC 3986 编码） */
    private static String encodePath(String key) {
        String[] segments = key.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            segments[i] = urlEncode(segments[i]);
        }
        return String.join("/", segments);
    }

    /** URL 编码：空格转 %20（URLEncoder 默认输出 +） */
    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static byte[] hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("HmacSHA256 不可用", ex);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return builder.toString();
    }
}
