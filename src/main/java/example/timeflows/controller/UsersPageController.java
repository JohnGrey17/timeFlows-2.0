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
import java.util.Comparator;

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
    public String users(@RequestParam(required = false) Long departmentId,
                        @RequestParam(required = false) Long divisionId,
                        @RequestParam(defaultValue = "department") String groupBy,
                        Authentication authentication, Model model) {
        User currentUser = userService.findByEmail(authentication.getName());
        boolean admin = currentUser.getRoles().contains(Role.ADMIN);
        Long effectiveDepartmentId = admin ? departmentId : currentUser.getDivision().getDepartment().getId();
        Long effectiveDivisionId = admin ? divisionId : currentUser.getDivision().getId();
        List<User> users = effectiveDivisionId != null
                ? userService.findActiveUsersByDivision(effectiveDivisionId)
                : effectiveDepartmentId != null
                ? userService.findActiveUsersByDepartment(effectiveDepartmentId)
                : userService.findActiveUsers();
        groupBy = "role".equals(groupBy) ? "role" : "department";
        Comparator<User> byName = Comparator
                .comparing((User user) -> user.getLastName() == null ? "" : user.getLastName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(user -> user.getFirstName() == null ? "" : user.getFirstName(), String.CASE_INSENSITIVE_ORDER);
        Comparator<User> comparator = "role".equals(groupBy)
                ? Comparator.comparingInt(this::roleRank).thenComparing(byName)
                : Comparator.comparing((User user) -> user.getDivision().getName(), String.CASE_INSENSITIVE_ORDER).thenComparing(byName);
        users = users.stream().sorted(comparator).toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", users);
        model.addAttribute("departments", admin ? departmentService.findAll() : List.of(currentUser.getDivision().getDepartment()));
        model.addAttribute("divisions", admin
                ? (effectiveDepartmentId == null ? List.of() : divisionService.findByDepartment(effectiveDepartmentId))
                : List.of(currentUser.getDivision()));
        model.addAttribute("selectedDepartmentId", effectiveDepartmentId);
        model.addAttribute("selectedDivisionId", effectiveDivisionId);
        model.addAttribute("activePage", "users");
        model.addAttribute("groupBy", groupBy);
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
    public String updateSalary(@PathVariable Long id, @RequestParam BigDecimal salary,
                               @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) Long divisionId,
                               @RequestParam(defaultValue = "department") String groupBy,
                               @RequestParam(required = false) String returnTo,
                               Authentication authentication) {
        assertCanManage(id, authentication);
        userService.updateSalary(id, salary);
        if ("summary".equals(returnTo)) return "redirect:/api/overtime/review?mode=division&view=summary";
        return "review".equals(returnTo) ? "redirect:/api/overtime/review?mode=division" : usersRedirect(departmentId, divisionId, groupBy);
    }

    @PostMapping("/api/users/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String deactivate(@PathVariable Long id, @RequestParam String reason, Authentication authentication,
                             @RequestParam(required = false) Long departmentId,
                             @RequestParam(required = false) Long divisionId,
                             @RequestParam(defaultValue = "department") String groupBy) {
        assertCanManage(id, authentication);
        userService.deactivate(id, reason);
        return usersRedirect(departmentId, divisionId, groupBy);
    }

    @PostMapping("/api/divisions/{divisionId}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignManager(@PathVariable Long divisionId, @RequestParam Long userId,
                                @RequestParam(required = false) Long departmentId,
                                @RequestParam(defaultValue = "department") String groupBy) {
        userService.assignDivisionManager(divisionId, userId);
        return usersRedirect(departmentId, divisionId, groupBy);
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

    private String usersRedirect(Long departmentId, Long divisionId, String groupBy) {
        StringBuilder redirect = new StringBuilder("redirect:/api/users?");
        if (departmentId != null) redirect.append("departmentId=").append(departmentId).append('&');
        if (divisionId != null) redirect.append("divisionId=").append(divisionId);
        if (redirect.charAt(redirect.length() - 1) != '?') redirect.append('&');
        redirect.append("groupBy=").append("role".equals(groupBy) ? "role" : "department");
        return redirect.toString();
    }

    private int roleRank(User user) {
        if (user.getRoles().contains(Role.ADMIN)) return 0;
        if (user.getRoles().contains(Role.MANAGER)) return 1;
        return 2;
    }
}
