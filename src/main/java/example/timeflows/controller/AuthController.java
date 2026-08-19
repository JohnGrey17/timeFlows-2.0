package example.timeflows.controller;

import example.timeflows.controller.dto.LoginRequest;
import example.timeflows.controller.dto.RegisterRequest;
import example.timeflows.exception.UserException;
import example.timeflows.security.JwtAuthenticationFilter;
import example.timeflows.security.JwtService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.MfaService;
import example.timeflows.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final DivisionService divisionService;
    private final example.timeflows.service.DepartmentService departmentService;
    private final Duration jwtExpiration;
    private final MfaService mfaService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            DivisionService divisionService,
            example.timeflows.service.DepartmentService departmentService,
            MfaService mfaService,
            @Value("${timeflows.jwt.expiration}") Duration jwtExpiration) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.divisionService = divisionService;
        this.departmentService = departmentService;
        this.mfaService = mfaService;
        this.jwtExpiration = jwtExpiration;
    }

    @GetMapping("/api/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/api/login")
    public String login(
            @Valid @ModelAttribute LoginRequest loginRequest,
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getEmail(), loginRequest.getPassword()));
            example.timeflows.model.User domainUser = mfaService.user(authentication.getName());
            if (domainUser.isMfaEnabled() || mfaService.isRequired(domainUser)) {
                String pendingToken = jwtService.generateMfaPendingToken(authentication.getName());
                response.addHeader(
                        HttpHeaders.SET_COOKIE,
                        ResponseCookie.from("TIMEFLOWS_MFA_PENDING", pendingToken)
                                .httpOnly(true)
                                .sameSite("Lax")
                                .path("/")
                                .maxAge(Duration.ofMinutes(5))
                                .build()
                                .toString());
                return domainUser.isMfaEnabled()
                        ? "redirect:/api/mfa/challenge"
                        : "redirect:/api/mfa/setup";
            }
            String token = jwtService.generateToken((UserDetails) authentication.getPrincipal());
            response.addHeader(
                    HttpHeaders.SET_COOKIE, createJwtCookie(token, jwtExpiration).toString());
            return "redirect:/api/dashboard";
        } catch (BadCredentialsException exception) {
            model.addAttribute("loginError", "Невірний email або пароль");
            return "auth/login";
        }
    }

    @GetMapping("/api/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("divisions", divisionService.findAll());
        return "auth/register";
    }

    @PostMapping("/api/register")
    public String register(
            @Valid @ModelAttribute RegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("divisions", divisionService.findAll());
            model.addAttribute("departments", departmentService.findAll());
            return "auth/register";
        }

        try {
            userService.register(registerRequest);
            return "redirect:/api/login?registered";
        } catch (UserException exception) {
            model.addAttribute("divisions", divisionService.findAll());
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("registerError", exception.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/api/logout")
    public String logout(HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        response.addHeader(HttpHeaders.SET_COOKIE, createJwtCookie("", Duration.ZERO).toString());
        return "redirect:/api/login?logout";
    }

    private ResponseCookie createJwtCookie(String token, Duration maxAge) {
        return ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
