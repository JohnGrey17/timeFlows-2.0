package example.timeflows.controller;

import example.timeflows.security.JwtAuthenticationFilter;
import example.timeflows.security.JwtService;
import example.timeflows.service.MfaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MfaController {
    private static final String PENDING_COOKIE = "TIMEFLOWS_MFA_PENDING";
    private final MfaService mfa;
    private final JwtService jwt;
    private final UserDetailsService userDetailsService;
    private final Duration jwtExpiration;

    public MfaController(
            MfaService mfa,
            JwtService jwt,
            UserDetailsService userDetailsService,
            @Value("${timeflows.jwt.expiration}") Duration jwtExpiration) {
        this.mfa = mfa;
        this.jwt = jwt;
        this.userDetailsService = userDetailsService;
        this.jwtExpiration = jwtExpiration;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String expiredMfaSession() {
        return "redirect:/api/login?mfaExpired";
    }

    @GetMapping("/api/mfa/challenge")
    public String challenge(HttpServletRequest request, Model model) {
        model.addAttribute("email", pendingEmail(request));
        return "auth/mfa-challenge";
    }

    @PostMapping("/api/mfa/challenge")
    public String verify(
            @RequestParam String code,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        String email = pendingEmail(request);
        if (!mfa.verify(email, code) && !mfa.useRecoveryCode(email, code)) {
            model.addAttribute("email", email);
            model.addAttribute("mfaError", "Невірний або прострочений код");
            return "auth/mfa-challenge";
        }
        completeLogin(email, response);
        return "redirect:/api/dashboard";
    }

    @GetMapping("/api/mfa/setup")
    public String setup(Authentication authentication, HttpServletRequest request, Model model) {
        String email = currentEmail(authentication, request);
        model.addAttribute("email", email);
        model.addAttribute("secret", mfa.prepare(email));
        model.addAttribute("qrCode", mfa.qrDataUri(email));
        return "auth/mfa-setup";
    }

    @PostMapping("/api/mfa/setup")
    public String enable(
            @RequestParam String code,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        String email = currentEmail(authentication, request);
        try {
            model.addAttribute("recoveryCodes", mfa.enable(email, code));
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken)
                completeLogin(email, response);
            return "auth/mfa-recovery";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("email", email);
            model.addAttribute("secret", mfa.prepare(email));
            model.addAttribute("qrCode", mfa.qrDataUri(email));
            model.addAttribute("mfaError", exception.getMessage());
            return "auth/mfa-setup";
        }
    }

    private String currentEmail(Authentication authentication, HttpServletRequest request) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken)
                ? authentication.getName()
                : pendingEmail(request);
    }

    private String pendingEmail(HttpServletRequest request) {
        String token =
                request.getCookies() == null
                        ? null
                        : Arrays.stream(request.getCookies())
                                .filter(cookie -> PENDING_COOKIE.equals(cookie.getName()))
                                .map(Cookie::getValue)
                                .findFirst()
                                .orElse(null);
        try {
            if (token == null || !jwt.isMfaPendingToken(token))
                throw new IllegalArgumentException();
            return jwt.extractUsername(token);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("MFA-сесія завершилася. Увійдіть знову.");
        }
    }

    private void completeLogin(String email, HttpServletResponse response) {
        String token = jwt.generateToken(userDetailsService.loadUserByUsername(email));
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, token, jwtExpiration));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(PENDING_COOKIE, "", Duration.ZERO));
    }

    private String cookie(String name, String value, Duration age) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(age)
                .build()
                .toString();
    }
}
