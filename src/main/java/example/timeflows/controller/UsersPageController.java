package example.timeflows.controller;

import example.timeflows.exception.UserException;
import example.timeflows.model.BusinessTag;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DirectorateService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.ManagementAccessService;
import example.timeflows.service.MfaService;
import example.timeflows.service.SubdivisionService;
import example.timeflows.service.UserService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UsersPageController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final ManagementAccessService accessService;
    private final MfaService mfaService;
    private final SubdivisionService subdivisionService;
    private final DirectorateService directorateService;

    public UsersPageController(
            UserService userService,
            DepartmentService departmentService,
            DivisionService divisionService,
            DirectorateService directorateService,
            SubdivisionService subdivisionService,
            ManagementAccessService accessService,
            MfaService mfaService) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.directorateService = directorateService;
        this.subdivisionService = subdivisionService;
        this.accessService = accessService;
        this.mfaService = mfaService;
    }

    @GetMapping("/api/users")
    @PreAuthorize("isAuthenticated()")
    public String users(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(defaultValue = "department") String groupBy,
            Authentication authentication,
            Model model) {
        User currentUser = userService.findByEmail(authentication.getName());
        if (!currentUser.getRoles().contains(Role.ADMIN)
                && !currentUser.getRoles().contains(Role.MANAGER)
                && !currentUser.getTags().contains(BusinessTag.SYS_ADMIN)) {
            throw new AccessDeniedException(
                    "Керування користувачами доступне лише адміністратору або менеджеру");
        }
        boolean admin = currentUser.getRoles().contains(Role.ADMIN);
        boolean globalUserManager = admin || currentUser.getTags().contains(BusinessTag.SYS_ADMIN);
        Long effectiveDepartmentId =
                globalUserManager
                        ? departmentId
                        : currentUser.getDivision().getDepartment().getId();
        Long effectiveDivisionId =
                globalUserManager ? divisionId : currentUser.getDivision().getId();
        Long effectiveDirectorateId = globalUserManager ? directorateId : null;
        Long effectiveSubdivisionId = globalUserManager ? subdivisionId : null;
        List<User> users =
                effectiveSubdivisionId != null
                        ? userService.findActiveUsersBySubdivision(effectiveSubdivisionId)
                        : effectiveDivisionId != null
                                ? userService.findActiveUsersByDivision(effectiveDivisionId)
                                : effectiveDirectorateId != null
                                        ? userService.findActiveUsersByDirectorate(
                                                effectiveDirectorateId)
                                        : effectiveDepartmentId != null
                                                ? userService.findActiveUsersByDepartment(
                                                        effectiveDepartmentId)
                                                : userService.findActiveUsers();
        groupBy = "role".equals(groupBy) ? "role" : "department";
        Comparator<User> byName =
                Comparator.comparing(
                                (User user) -> user.getLastName() == null ? "" : user.getLastName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                user -> user.getFirstName() == null ? "" : user.getFirstName(),
                                String.CASE_INSENSITIVE_ORDER);
        Comparator<User> comparator =
                "role".equals(groupBy)
                        ? Comparator.comparingInt(this::roleRank).thenComparing(byName)
                        : Comparator.comparing(
                                        (User user) -> user.getDivision().getName(),
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(byName);
        users = users.stream().sorted(comparator).toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", users);
        model.addAttribute(
                "departments",
                globalUserManager
                        ? departmentService.findAll()
                        : List.of(currentUser.getDivision().getDepartment()));
        model.addAttribute(
                "divisions",
                globalUserManager
                        ? (effectiveDepartmentId == null
                                ? List.of()
                                : divisionService.findByDepartment(effectiveDepartmentId))
                        : List.of(currentUser.getDivision()));
        model.addAttribute("selectedDepartmentId", effectiveDepartmentId);
        model.addAttribute("selectedDirectorateId", effectiveDirectorateId);
        model.addAttribute("selectedDivisionId", effectiveDivisionId);
        model.addAttribute("selectedSubdivisionId", effectiveSubdivisionId);
        model.addAttribute(
                "directorates",
                globalUserManager && effectiveDepartmentId != null
                        ? directorateService.findByDepartment(effectiveDepartmentId)
                        : List.of());
        model.addAttribute(
                "filterDivisions",
                globalUserManager && effectiveDirectorateId != null
                        ? divisionService.findByDirectorate(effectiveDirectorateId)
                        : List.of());
        model.addAttribute(
                "filterSubdivisions",
                globalUserManager && effectiveDivisionId != null
                        ? subdivisionService.findByDivision(effectiveDivisionId)
                        : List.of());
        model.addAttribute(
                "selectedDivision",
                globalUserManager
                        ? (effectiveDivisionId == null
                                ? null
                                : divisionService.findById(effectiveDivisionId))
                        : currentUser.getDivision());
        model.addAttribute("activePage", "users");
        model.addAttribute("groupBy", groupBy);
        model.addAttribute("allDivisions", admin ? divisionService.findAll() : List.of());
        model.addAttribute("allSubdivisions", admin ? subdivisionService.findAll() : List.of());
        return "admin/users";
    }

    @GetMapping("/api/users/deactivated")
    @PreAuthorize("hasAnyRole('ADMIN','SYS_ADMIN')")
    public String deactivatedUsers(Authentication authentication, Model model) {
        model.addAttribute("currentUser", userService.findByEmail(authentication.getName()));
        model.addAttribute("users", userService.findDeactivatedUsers());
        model.addAttribute("activePage", "users");
        return "admin/deactivated-users";
    }

    @PostMapping("/api/users/{id}/salary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String updateSalary(
            @PathVariable Long id,
            @RequestParam BigDecimal salary,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "department") String groupBy,
            @RequestParam(required = false) String returnTo,
            Authentication authentication) {
        accessService.assertCanManageUser(authentication.getName(), id);
        userService.updateSalary(id, salary);
        if ("summary".equals(returnTo))
            return "redirect:/api/overtime/review?mode=division&view=summary";
        return "review".equals(returnTo)
                ? "redirect:/api/overtime/review?mode=division"
                : usersRedirect(departmentId, divisionId, groupBy);
    }

    @PostMapping("/api/users/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SYS_ADMIN')")
    public String deactivate(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "department") String groupBy) {
        User currentUser = userService.findByEmail(authentication.getName());
        if (currentUser.getId().equals(id)) {
            throw new UserException("Не можна деактивувати власний обліковий запис");
        }
        if (currentUser.getTags().contains(BusinessTag.SYS_ADMIN)
                && !currentUser.getRoles().contains(Role.ADMIN)
                && userService.findById(id).getRoles().contains(Role.ADMIN)) {
            throw new UserException("SYS_ADMIN не може деактивувати адміністратора");
        }
        if (!currentUser.getTags().contains(BusinessTag.SYS_ADMIN)) {
            accessService.assertCanManageUser(authentication.getName(), id);
        }
        userService.deactivate(id, reason);
        return usersRedirect(departmentId, divisionId, groupBy);
    }

    @PostMapping("/api/divisions/{divisionId}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignManager(
            @PathVariable Long divisionId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "department") String groupBy) {
        userService.assignDivisionManager(divisionId, userId);
        return usersRedirect(departmentId, divisionId, groupBy);
    }

    @PostMapping("/api/users/{id}/organization")
    @PreAuthorize("hasRole('ADMIN')")
    public String moveUser(
            @PathVariable Long id,
            @RequestParam Long targetDivisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "department") String groupBy) {
        userService.moveToOrganization(id, targetDivisionId, subdivisionId);
        return usersRedirect(departmentId, divisionId, groupBy);
    }

    @PostMapping("/api/users/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','SYS_ADMIN')")
    public String updateRoles(
            @PathVariable Long id,
            @RequestParam(required = false) Set<Role> roles,
            Authentication authentication,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(defaultValue = "department") String groupBy) {
        userService.updateRoles(id, roles, authentication.getName());
        return usersRedirect(departmentId, directorateId, divisionId, subdivisionId, groupBy);
    }

    @PostMapping("/api/users/{id}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateTags(
            @PathVariable Long id,
            @RequestParam(required = false) Set<BusinessTag> tags,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(defaultValue = "department") String groupBy) {
        userService.updateTags(id, tags);
        return usersRedirect(departmentId, directorateId, divisionId, subdivisionId, groupBy);
    }

    @PostMapping("/api/divisions/{id}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateDivisionTags(
            @PathVariable Long id,
            @RequestParam(required = false) Set<BusinessTag> tags,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "department") String groupBy) {
        userService.updateDivisionTags(id, tags);
        return usersRedirect(departmentId, id, groupBy);
    }

    @PostMapping("/api/users/{id}/mfa/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetMfa(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "department") String groupBy) {
        mfaService.resetByAdmin(id, authentication.getName());
        return usersRedirect(departmentId, divisionId, groupBy);
    }

    private String usersRedirect(Long departmentId, Long divisionId, String groupBy) {
        return usersRedirect(departmentId, null, divisionId, null, groupBy);
    }

    private String usersRedirect(
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            String groupBy) {
        StringBuilder redirect = new StringBuilder("redirect:/api/users?");
        if (departmentId != null) redirect.append("departmentId=").append(departmentId).append('&');
        if (directorateId != null)
            redirect.append("directorateId=").append(directorateId).append('&');
        if (divisionId != null) redirect.append("divisionId=").append(divisionId).append('&');
        if (subdivisionId != null)
            redirect.append("subdivisionId=").append(subdivisionId).append('&');
        redirect.append("groupBy=").append("role".equals(groupBy) ? "role" : "department");
        return redirect.toString();
    }

    private int roleRank(User user) {
        if (user.getRoles().contains(Role.ADMIN)) return 0;
        if (user.getRoles().contains(Role.MANAGER)) return 1;
        return 2;
    }
}
