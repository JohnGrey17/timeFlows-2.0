package example.timeflows.controller;

import example.timeflows.controller.dto.PasswordChangeRequest;
import example.timeflows.controller.dto.ProfileRequest;
import example.timeflows.exception.UserException;
import example.timeflows.service.EmployeePageService;
import example.timeflows.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmployeePageController {

    private final UserService userService;
    private final EmployeePageService employeePageService;

    public EmployeePageController(
            UserService userService, EmployeePageService employeePageService) {
        this.userService = userService;
        this.employeePageService = employeePageService;
    }

    @GetMapping("/api/overtime")
    public String overtime(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication,
            Model model) {
        model.addAllAttributes(
                employeePageService.overtimePage(authentication.getName(), year, month));
        return "employee/overtime";
    }

    @GetMapping("/api/settings")
    public String settings(Authentication authentication, Model model) {
        model.addAllAttributes(
                employeePageService.settingsPage(authentication.getName(), null, null));
        return "employee/settings";
    }

    @PostMapping("/api/settings/profile")
    public String updateProfile(
            @Valid @ModelAttribute ProfileRequest profileRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAllAttributes(
                    employeePageService.settingsPage(
                            authentication.getName(), profileRequest, null));
            return "employee/settings";
        }
        userService.updateProfile(
                authentication.getName(),
                profileRequest.getFirstName(),
                profileRequest.getLastName());
        return "redirect:/api/settings?profileUpdated";
    }

    @PostMapping("/api/settings/password")
    public String changePassword(
            @Valid @ModelAttribute PasswordChangeRequest passwordChangeRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {
        ProfileRequest profileRequest = new ProfileRequest();
        if (bindingResult.hasErrors()) {
            model.addAllAttributes(
                    employeePageService.settingsPage(
                            authentication.getName(), profileRequest, passwordChangeRequest));
            return "employee/settings";
        }
        try {
            userService.changePassword(
                    authentication.getName(),
                    passwordChangeRequest.getCurrentPassword(),
                    passwordChangeRequest.getNewPassword(),
                    passwordChangeRequest.getConfirmPassword());
            return "redirect:/api/settings?passwordUpdated";
        } catch (UserException exception) {
            model.addAllAttributes(
                    employeePageService.settingsPage(
                            authentication.getName(), profileRequest, passwordChangeRequest));
            model.addAttribute("passwordError", exception.getMessage());
            return "employee/settings";
        }
    }
}
