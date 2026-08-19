package example.timeflows.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtService(
            @Value("${timeflows.jwt.secret}") String secret,
            @Value("${timeflows.jwt.expiration}") Duration expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(UserDetails userDetails) {
        return generate(userDetails.getUsername(), expiration, "AUTH");
    }

    public String generateMfaPendingToken(String username) {
        return generate(username, Duration.ofMinutes(15), "MFA_PENDING");
    }

    private String generate(String username, Duration lifetime, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && "AUTH".equals(extractClaims(token).get("type", String.class))
                && !isExpired(token);
    }

    public boolean isMfaPendingToken(String token) {
        return "MFA_PENDING".equals(extractClaims(token).get("type", String.class))
                && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
