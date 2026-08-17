package example.timeflows.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTests {

    private static final String SECRET =
            "test-secret-that-is-long-enough-for-hmac-sha-signing-123456789";

    @Test
    void generatedTokenContainsUsernameAndIsValidForSameUser() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        UserDetails user =
                User.withUsername("employee@vyriy.com")
                        .password("password")
                        .roles("EMPLOYEE")
                        .build();

        String token = service.generateToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("employee@vyriy.com");
        assertThat(service.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentUserAndAfterExpiration() {
        UserDetails owner =
                User.withUsername("owner@vyriy.com").password("password").roles("EMPLOYEE").build();
        UserDetails other =
                User.withUsername("other@vyriy.com").password("password").roles("EMPLOYEE").build();
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));

        assertThat(service.isTokenValid(service.generateToken(owner), other)).isFalse();

        JwtService expiringService = new JwtService(SECRET, Duration.ofMillis(-1));
        String expired = expiringService.generateToken(owner);
        assertThatThrownBy(() -> expiringService.isTokenValid(expired, owner))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService issuer = new JwtService(SECRET, Duration.ofHours(1));
        JwtService verifier =
                new JwtService(
                        "different-test-secret-long-enough-for-hmac-signing-987654321",
                        Duration.ofHours(1));
        UserDetails user =
                User.withUsername("employee@vyriy.com")
                        .password("password")
                        .roles("EMPLOYEE")
                        .build();

        assertThatThrownBy(() -> verifier.extractUsername(issuer.generateToken(user)))
                .isInstanceOf(JwtException.class);
    }
}
