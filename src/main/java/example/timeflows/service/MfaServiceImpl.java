package example.timeflows.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import example.timeflows.model.MfaAuditEvent;
import example.timeflows.model.MfaRecoveryCode;
import example.timeflows.model.User;
import example.timeflows.repository.MfaAuditEventRepository;
import example.timeflows.repository.MfaRecoveryCodeRepository;
import example.timeflows.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaServiceImpl implements MfaService {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final UserRepository users;
    private final MfaRecoveryCodeRepository recoveryCodes;
    private final PasswordEncoder passwordEncoder;
    private final MfaAuditEventRepository auditEvents;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec encryptionKey;
    private final String issuer;

    public MfaServiceImpl(
            UserRepository users,
            MfaRecoveryCodeRepository recoveryCodes,
            MfaAuditEventRepository auditEvents,
            PasswordEncoder passwordEncoder,
            @Value("${timeflows.mfa.encryption-key}") String encryptionKey,
            @Value("${timeflows.mfa.issuer}") String issuer) {
        this.users = users;
        this.recoveryCodes = recoveryCodes;
        this.auditEvents = auditEvents;
        this.passwordEncoder = passwordEncoder;
        this.encryptionKey = new SecretKeySpec(sha256(encryptionKey), "AES");
        this.issuer = issuer;
    }

    public boolean isRequired(User user) {
        return true;
    }

    @Transactional
    public String prepare(String email) {
        User user = user(email);
        if (user.getMfaSecret() == null) {
            byte[] value = new byte[20];
            random.nextBytes(value);
            user.setMfaSecret(encrypt(base32(value)));
            users.save(user);
        }
        return secret(user);
    }

    public String qrDataUri(String email) {
        try {
            String secret = prepare(email);
            String label = encode(issuer) + ":" + encode(email);
            String uri =
                    "otpauth://totp/"
                            + label
                            + "?secret="
                            + secret
                            + "&issuer="
                            + encode(issuer)
                            + "&algorithm=SHA1&digits=6&period=30";
            BitMatrix matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 260, 260);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Не вдалося створити QR-код MFA", exception);
        }
    }

    public boolean verify(String email, String code) {
        User user = user(email);
        if (code == null || !code.matches("\\d{6}")) return false;
        long counter = Instant.now().getEpochSecond() / 30;
        for (long offset = -1; offset <= 1; offset++) {
            if (constantEquals(code, totp(secret(user), counter + offset))) return true;
        }
        return false;
    }

    @Transactional
    public List<String> enable(String email, String code) {
        User user = user(email);
        long counter = matchingCounter(user, code);
        if (counter < 0) throw new IllegalArgumentException("Невірний код Google Authenticator");
        if (user.getMfaEnrollmentCounter() == null) {
            user.setMfaEnrollmentCounter(counter);
            users.save(user);
            return List.of();
        }
        if (counter <= user.getMfaEnrollmentCounter())
            throw new IllegalArgumentException(
                    "Дочекайтеся нового коду Google Authenticator і введіть його повторно");
        user.setMfaEnabled(true);
        user.setMfaEnabledAt(LocalDateTime.now());
        user.setMfaEnrollmentCounter(null);
        recoveryCodes.deleteByUserId(user.getId());
        List<String> plainCodes = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            String plain = randomCode();
            MfaRecoveryCode stored = new MfaRecoveryCode();
            stored.setUser(user);
            stored.setCodeHash(passwordEncoder.encode(normalizeRecovery(plain)));
            recoveryCodes.save(stored);
            plainCodes.add(plain);
        }
        user.setMfaRecoveryBundle(encrypt(String.join("\n", plainCodes)));
        users.save(user);
        return plainCodes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> recoveryCodesForConfirmation(String email) {
        User user = user(email);
        if (user.getMfaRecoveryBundle() == null)
            throw new IllegalArgumentException("Recovery-коди вже підтверджені");
        return List.of(decrypt(user.getMfaRecoveryBundle()).split("\\n"));
    }

    @Override
    @Transactional
    public void confirmRecoveryCodes(String email) {
        User user = user(email);
        if (!user.isMfaEnabled() || user.getMfaRecoveryBundle() == null)
            throw new IllegalArgumentException("Немає recovery-кодів для підтвердження");
        user.setMfaRecoveryBundle(null);
        users.save(user);
    }

    @Transactional
    public boolean useRecoveryCode(String email, String code) {
        User user = user(email);
        String normalized = normalizeRecovery(code);
        return recoveryCodes.findByUserIdAndUsedAtIsNull(user.getId()).stream()
                .filter(item -> passwordEncoder.matches(normalized, item.getCodeHash()))
                .findFirst()
                .map(
                        item -> {
                            item.setUsedAt(LocalDateTime.now());
                            recoveryCodes.save(item);
                            return true;
                        })
                .orElse(false);
    }

    @Transactional
    public void disable(String email, String password, String code) {
        User user = user(email);
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new IllegalArgumentException("Невірний пароль");
        if (!verify(email, code) && !useRecoveryCode(email, code))
            throw new IllegalArgumentException("Невірний MFA-код");
        if (isRequired(user))
            throw new IllegalArgumentException(
                    "Для адміністратора та менеджера MFA є обов'язковою");
        recoveryCodes.deleteByUserId(user.getId());
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaEnabledAt(null);
        users.save(user);
    }

    public User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено"));
    }

    @Override
    @Transactional
    public void resetByAdmin(Long userId, String adminEmail) {
        User admin = user(adminEmail);
        if (!admin.getRoles().contains(example.timeflows.model.Role.ADMIN))
            throw new IllegalArgumentException("Скинути MFA може лише адміністратор");
        User target =
                users.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено"));
        if (target.getId().equals(admin.getId()))
            throw new IllegalArgumentException(
                    "Адміністратор не може самостійно скинути власну MFA");
        recoveryCodes.deleteByUserId(target.getId());
        target.setMfaEnabled(false);
        target.setMfaSecret(null);
        target.setMfaEnabledAt(null);
        target.setMfaEnrollmentCounter(null);
        target.setMfaRecoveryBundle(null);
        users.save(target);
        MfaAuditEvent event = new MfaAuditEvent();
        event.setActorEmail(adminEmail);
        event.setTargetEmail(target.getEmail());
        event.setAction("MFA_RESET_BY_ADMIN");
        auditEvents.save(event);
    }

    private long matchingCounter(User user, String code) {
        if (code == null || !code.matches("\\d{6}")) return -1;
        long current = Instant.now().getEpochSecond() / 30;
        return constantEquals(code, totp(secret(user), current)) ? current : -1;
    }

    private String secret(User user) {
        if (user.getMfaSecret() == null) throw new IllegalStateException("MFA не налаштовано");
        return decrypt(user.getMfaSecret());
    }

    private String totp(String secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 15;
            int binary =
                    ((hash[offset] & 127) << 24)
                            | ((hash[offset + 1] & 255) << 16)
                            | ((hash[offset + 2] & 255) << 8)
                            | (hash[offset + 3] & 255);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception exception) {
            throw new IllegalStateException("Не вдалося перевірити TOTP", exception);
        }
    }

    private String encrypt(String value) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder()
                    .encodeToString(
                            ByteBuffer.allocate(iv.length + encrypted.length)
                                    .put(iv)
                                    .put(encrypted)
                                    .array());
        } catch (Exception exception) {
            throw new IllegalStateException("Не вдалося зашифрувати MFA secret", exception);
        }
    }

    private String decrypt(String value) {
        try {
            ByteBuffer data = ByteBuffer.wrap(Base64.getDecoder().decode(value));
            byte[] iv = new byte[12];
            data.get(iv);
            byte[] encrypted = new byte[data.remaining()];
            data.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Не вдалося розшифрувати MFA secret", exception);
        }
    }

    private String base32(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 255);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32.charAt((buffer >> (bits -= 5)) & 31));
            }
        }
        if (bits > 0) result.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return result.toString();
    }

    private byte[] base32Decode(String value) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char character : value.toUpperCase().toCharArray()) {
            int current = BASE32.indexOf(character);
            if (current < 0) continue;
            buffer = (buffer << 5) | current;
            bits += 5;
            if (bits >= 8) result.write((buffer >> (bits -= 8)) & 255);
        }
        return result.toByteArray();
    }

    private String randomCode() {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase();
        return value.substring(0, 4) + "-" + value.substring(4, 8);
    }

    private String normalizeRecovery(String value) {
        return value == null ? "" : value.replace("-", "").trim().toUpperCase();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean constantEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
