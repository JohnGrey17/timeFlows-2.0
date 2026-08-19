package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.MfaAuditEventRepository;
import example.timeflows.repository.MfaRecoveryCodeRepository;
import example.timeflows.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MfaServiceTests {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Test
    void generatedSecretProducesValidGoogleAuthenticatorCompatibleTotp() throws Exception {
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        user.setEmail("user@vyriy.com");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        MfaService service =
                new MfaServiceImpl(
                        users,
                        mock(MfaRecoveryCodeRepository.class),
                        mock(example.timeflows.repository.MfaAuditEventRepository.class),
                        new BCryptPasswordEncoder(),
                        "test-encryption-key",
                        "timeFlows");

        String secret = service.prepare(user.getEmail());
        String code = totp(secret, Instant.now().getEpochSecond() / 30);

        assertThat(secret).matches("[A-Z2-7]{32}");
        assertThat(service.verify(user.getEmail(), code)).isTrue();
        assertThat(service.qrDataUri(user.getEmail())).startsWith("data:image/png;base64,");
        assertThat(service.enable(user.getEmail(), code)).isEmpty();
        assertThatThrownBy(() -> service.enable(user.getEmail(), code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("нового коду");

        user.setMfaEnrollmentCounter(user.getMfaEnrollmentCounter() - 1);
        assertThat(service.enable(user.getEmail(), code)).hasSize(8);
        assertThat(user.isMfaEnabled()).isTrue();
        assertThat(service.recoveryCodesForConfirmation(user.getEmail())).hasSize(8);
        service.confirmRecoveryCodes(user.getEmail());
        assertThat(user.getMfaRecoveryBundle()).isNull();
    }

    @Test
    void adminResetClearsMfaAndWritesAuditEvent() {
        UserRepository users = mock(UserRepository.class);
        MfaRecoveryCodeRepository recoveryCodes = mock(MfaRecoveryCodeRepository.class);
        MfaAuditEventRepository auditEvents = mock(MfaAuditEventRepository.class);
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@vyriy.com");
        admin.setRoles(Set.of(Role.ADMIN));
        User target = new User();
        target.setId(2L);
        target.setEmail("employee@vyriy.com");
        target.setMfaEnabled(true);
        target.setMfaSecret("encrypted");
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(users.findById(target.getId())).thenReturn(Optional.of(target));
        MfaService service =
                new MfaServiceImpl(
                        users,
                        recoveryCodes,
                        auditEvents,
                        new BCryptPasswordEncoder(),
                        "test-encryption-key",
                        "timeFlows");

        service.resetByAdmin(target.getId(), admin.getEmail());

        assertThat(target.isMfaEnabled()).isFalse();
        assertThat(target.getMfaSecret()).isNull();
        verify(recoveryCodes).deleteByUserId(target.getId());
        verify(auditEvents).save(org.mockito.ArgumentMatchers.any());
    }

    private String totp(String secret, long counter) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
        byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
        int offset = hash[hash.length - 1] & 15;
        int binary =
                ((hash[offset] & 127) << 24)
                        | ((hash[offset + 1] & 255) << 16)
                        | ((hash[offset + 2] & 255) << 8)
                        | (hash[offset + 3] & 255);
        return String.format("%06d", binary % 1_000_000);
    }

    private byte[] decode(String value) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char character : value.toCharArray()) {
            buffer = (buffer << 5) | BASE32.indexOf(character);
            bits += 5;
            if (bits >= 8) result.write((buffer >> (bits -= 8)) & 255);
        }
        return result.toByteArray();
    }
}
