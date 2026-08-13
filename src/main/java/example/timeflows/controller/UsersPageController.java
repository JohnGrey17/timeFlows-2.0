package example.timeflows.controller;

import example.timeflows.exception.UserException;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class UsersPageController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;

    public UsersPageController(
            UserService userService,
            DepartmentService departmentService,
            DivisionService divisionService
    ) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String users(@RequestParam(required = false) Long divisionId, Authentication authentication, Model model) {
        User currentUser = userService.findByEmail(authentication.getName());
        boolean admin = currentUser.getRoles().contains(Role.ADMIN);
        Long effectiveDivisionId = admin ? divisionId : currentUser.getDivision().getId();
        List<User> users = effectiveDivisionId == null
                ? userService.findActiveUsers()
                : userService.findActiveUsersByDivision(effectiveDivisionId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", users);
        model.addAttribute("departments", admin ? departmentService.findAll() : List.of(currentUser.getDivision().getDepartment()));
        model.addAttribute("divisions", admin ? divisionService.findAll() : List.of(currentUser.getDivision()));
        model.addAttribute("selectedDivisionId", effectiveDivisionId);
        model.addAttribute("activePage", "users");
        return "admin/users";
    }

    @GetMapping("/api/users/deactivated")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivatedUsers(Authentication authentication, Model model) {
        model.addAttribute("currentUser", userService.findByEmail(authentication.getName()));
        model.addAttribute("users", userService.findDeactivatedUsers());
        model.addAttribute("activePage", "users");
        return "admin/deactivated-users";
    }

    @PostMapping("/api/users/{id}/salary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String updateSalary(@PathVariable Long id, @RequestParam BigDecimal salary, Authentication authentication) {
        assertCanManage(id, authentication);
        userService.updateSalary(id, salary);
        return "redirect:/api/users";
    }

    @PostMapping("/api/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id, @RequestParam String reason) {
        userService.deactivate(id, reason);
        return "redirect:/api/users";
    }

    @PostMapping("/api/divisions/{divisionId}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignManager(@PathVariable Long divisionId, @RequestParam Long userId) {
        userService.assignDivisionManager(divisionId, userId);
        return "redirect:/api/users?divisionId=" + divisionId;
    }

    private void assertCanManage(Long userId, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            return;
        }
        User targetUser = userService.findById(userId);
        if (!targetUser.getDivision().getId().equals(currentUser.getDivision().getId())) {
            throw new UserException("Керівник може редагувати тільки користувачів свого відділу");
        }
    }
}
