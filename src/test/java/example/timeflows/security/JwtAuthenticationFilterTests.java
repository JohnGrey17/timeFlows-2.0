package example.timeflows.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    private static final String SECRET =
            "test-secret-that-is-long-enough-for-hmac-sha-signing-123456789";

    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService(SECRET, Duration.ofHours(1));
        example.timeflows.service.MfaService mfaService =
                mock(example.timeflows.service.MfaService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, mfaService);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerTokenAuthenticatesRequest() throws Exception {
        UserDetails user = user();
        String token = jwtService.generateToken(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo(user.getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void cookieTokenAuthenticatesRequest() throws Exception {
        UserDetails user = user();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(
                        JwtAuthenticationFilter.JWT_COOKIE_NAME, jwtService.generateToken(user)));
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void missingOrInvalidTokenContinuesWithoutAuthentication() throws Exception {
        MockHttpServletRequest missing = new MockHttpServletRequest();
        filter.doFilterInternal(missing, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest invalid = new MockHttpServletRequest();
        invalid.addHeader("Authorization", "Bearer invalid-token");
        filter.doFilterInternal(invalid, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never())
                .loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    private UserDetails user() {
        return User.withUsername("employee@vyriy.com")
                .password("password")
                .roles("EMPLOYEE")
                .build();
    }
}
