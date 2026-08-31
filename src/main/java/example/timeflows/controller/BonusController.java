package example.timeflows.controller;

import example.timeflows.model.*;
import example.timeflows.service.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class BonusController {
    private final BonusService bonusService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final DirectorateService directorateService;
    private final SubdivisionService subdivisionService;
    private final OvertimeViewService overtimeViewService;
    private final ManagementAccessService accessService;

    public BonusController(
            BonusService bonusService,
            UserService userService,
            DepartmentService departmentService,
            DivisionService divisionService,
            DirectorateService directorateService,
            SubdivisionService subdivisionService,
            OvertimeViewService overtimeViewService,
            ManagementAccessService accessService) {
        this.bonusService = bonusService;
        this.userService = userService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.directorateService = directorateService;
        this.subdivisionService = subdivisionService;
        this.overtimeViewService = overtimeViewService;
        this.accessService = accessService;
    }

    @GetMapping("/api/bonuses")
    @PreAuthorize("isAuthenticated()")
    public String page(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) BonusStatus status,
            Authentication auth,
            Model model) {
        User current = userService.findByEmail(auth.getName());
        assertCanOpenBonusModule(current);
        YearMonth selected =
                year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
        boolean admin = current.getRoles().contains(Role.ADMIN);
        boolean sysAdmin = current.getTags().contains(BusinessTag.SYS_ADMIN);
        Long effectiveDepartmentId = departmentId;
        Long effectiveDivisionId = divisionId;
        Long effectiveDirectorateId = directorateId;
        Long effectiveSubdivisionId = subdivisionId;
        List<Bonus> bonuses =
                effectiveDivisionId != null
                        ? bonusService.findDivisionMonth(effectiveDivisionId, selected)
                        : bonusService.findMonth(selected);
        if (effectiveDepartmentId != null)
            bonuses =
                    bonuses.stream()
                            .filter(
                                    b ->
                                            b.getUser()
                                                    .getDivision()
                                                    .getDepartment()
                                                    .getId()
                                                    .equals(effectiveDepartmentId))
                            .toList();
        if (effectiveDirectorateId != null)
            bonuses =
                    bonuses.stream()
                            .filter(
                                    b ->
                                            b.getUser().getDivision().getDirectorate() != null
                                                    && b.getUser()
                                                            .getDivision()
                                                            .getDirectorate()
                                                            .getId()
                                                            .equals(effectiveDirectorateId))
                            .toList();
        if (effectiveSubdivisionId != null)
            bonuses =
                    bonuses.stream()
                            .filter(
                                    b ->
                                            b.getUser().getSubdivision() != null
                                                    && b.getUser()
                                                            .getSubdivision()
                                                            .getId()
                                                            .equals(effectiveSubdivisionId))
                            .toList();
        if (status != null)
            bonuses = bonuses.stream().filter(b -> b.getStatus() == status).toList();
        model.addAttribute("currentUser", current);
        model.addAttribute("selectedMonth", selected);
        model.addAttribute("months", overtimeViewService.monthOptions());
        model.addAttribute("bonuses", bonuses);
        model.addAttribute("categories", bonusService.findCategories());
        model.addAttribute(
                "quarterlyRecipients",
                userService.findActiveUsers().stream()
                        .filter(user -> !hasProjectManagerTag(user))
                        .sorted(
                                Comparator.comparing(
                                                (User user) ->
                                                        user.getLastName() == null
                                                                ? ""
                                                                : user.getLastName(),
                                                String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(
                                                user ->
                                                        user.getFirstName() == null
                                                                ? ""
                                                                : user.getFirstName(),
                                                String.CASE_INSENSITIVE_ORDER))
                        .toList());
        model.addAttribute("quarterlyDirectorates", directorateService.findAll());
        model.addAttribute("quarterlyDivisions", divisionService.findAll());
        model.addAttribute("quarterlySubdivisions", subdivisionService.findAll());
        model.addAttribute(
                "users",
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
                                                : userService.findActiveUsers());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute(
                "divisions",
                effectiveDepartmentId == null
                        ? List.of()
                        : divisionService.findByDepartment(effectiveDepartmentId));
        model.addAttribute("selectedDepartmentId", effectiveDepartmentId);
        model.addAttribute("selectedDirectorateId", effectiveDirectorateId);
        model.addAttribute("selectedDivisionId", effectiveDivisionId);
        model.addAttribute("selectedSubdivisionId", effectiveSubdivisionId);
        model.addAttribute(
                "directorates",
                effectiveDepartmentId != null
                        ? directorateService.findByDepartment(effectiveDepartmentId)
                        : List.of());
        model.addAttribute(
                "filterDivisions",
                effectiveDirectorateId != null
                        ? divisionService.findByDirectorate(effectiveDirectorateId)
                        : List.of());
        model.addAttribute(
                "subdivisions",
                effectiveDivisionId != null
                        ? subdivisionService.findByDivision(effectiveDivisionId)
                        : List.of());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("admin", admin);
        model.addAttribute("sysAdmin", sysAdmin);
        model.addAttribute("canApproveBonuses", !sysAdmin);
        return "manager/bonuses";
    }

    @GetMapping("/api/bonus-categories")
    @ResponseBody
    public List<Map<String, Object>> categories() {
        return bonusService.findCategories().stream()
                .map(
                        c ->
                                Map.<String, Object>of(
                                        "id", c.getId(), "name", c.getName(), "type", c.getType()))
                .toList();
    }

    @GetMapping("/api/bonuses/{id}/details")
    @ResponseBody
    public Map<String, Object> details(@PathVariable Long id, Authentication auth) {
        accessService.assertCanEditBonus(auth.getName(), id);
        Bonus b = bonusService.find(id);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", b.getId());
        result.put("type", b.getType());
        if (b.getCategory() != null) {
            result.put("categoryId", b.getCategory().getId());
            result.put("category", b.getCategory().getName());
        } else {
            result.put("category", b.getType().name());
        }
        return result;
    }

    @PostMapping("/api/bonuses")
    public String create(
            @RequestParam Long userId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "MONTHLY") BonusType type,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String returnTo,
            Authentication auth,
            RedirectAttributes ra) {
        User current = userService.findByEmail(auth.getName());
        accessService.assertCanManage(current, userService.findById(userId));
        try {
            bonusService.create(userId, categoryId, type, amount, description, auth.getName());
            ra.addFlashAttribute("success", "Бонус створено та відправлено на погодження");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("bonusError", e.getMessage());
        }
        return "review".equals(returnTo)
                ? "redirect:/api/overtime/review?mode=division&openBonusUserId=" + userId
                : redirect(returnTo);
    }

    @PostMapping("/api/bonuses/{id}/update")
    public String update(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String returnTo,
            Authentication auth) {
        accessService.assertCanEditBonus(auth.getName(), id);
        bonusService.update(id, categoryId, amount, description, true);
        return redirect(returnTo);
    }

    @PostMapping("/api/bonuses/{id}/category")
    public String updateCategory(
            @PathVariable Long id, @RequestParam Long categoryId, Authentication auth) {
        accessService.assertCanEditBonus(auth.getName(), id);
        Bonus b = bonusService.find(id);
        if (b.getType() != BonusType.MONTHLY)
            throw new IllegalArgumentException("KPI та квартальний бонус не мають категорії");
        bonusService.update(id, categoryId, b.getAmount(), b.getDescription(), true);
        return "redirect:/api/bonuses";
    }

    @PostMapping("/api/bonuses/{id}/amount")
    public String updateAmount(
            @PathVariable Long id, @RequestParam BigDecimal amount, Authentication auth) {
        accessService.assertCanEditBonus(auth.getName(), id);
        Bonus b = bonusService.find(id);
        bonusService.update(
                id,
                b.getCategory() == null ? null : b.getCategory().getId(),
                amount,
                b.getDescription(),
                true);
        return "redirect:/api/bonuses";
    }

    @PostMapping("/api/bonuses/{id}/description")
    public String updateDescription(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            Authentication auth) {
        accessService.assertCanEditBonus(auth.getName(), id);
        Bonus b = bonusService.find(id);
        bonusService.update(
                id,
                b.getCategory() == null ? null : b.getCategory().getId(),
                b.getAmount(),
                description,
                true);
        return "redirect:/api/bonuses";
    }

    @PostMapping("/api/bonuses/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo,
            Authentication auth,
            RedirectAttributes ra) {
        accessService.assertCanEditBonus(auth.getName(), id);
        try {
            bonusService.delete(id, true);
        } catch (IllegalArgumentException exception) {
            ra.addFlashAttribute("bonusError", exception.getMessage());
        }
        return redirect(returnTo);
    }

    @PostMapping("/api/bonuses/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public String approve(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String returnTo,
            Authentication auth) {
        assertCanApproveBonuses(userService.findByEmail(auth.getName()));
        bonusService.decide(id, BonusStatus.APPROVED, comment);
        return redirect(returnTo);
    }

    @PostMapping("/api/bonuses/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public String reject(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String returnTo,
            Authentication auth) {
        assertCanApproveBonuses(userService.findByEmail(auth.getName()));
        bonusService.decide(id, BonusStatus.REJECTED, comment);
        return redirect(returnTo);
    }

    @PostMapping("/api/bonus-categories")
    @PreAuthorize("hasAnyRole('ADMIN','SYS_ADMIN')")
    public String createCategory(
            @RequestParam String name,
            @RequestParam(defaultValue = "MONTHLY") BonusType type,
            RedirectAttributes ra) {
        try {
            bonusService.createCategory(name, type);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("bonusError", e.getMessage());
        }
        return "redirect:/api/bonuses";
    }

    @PostMapping("/api/bonus-categories/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN','SYS_ADMIN')")
    public String updateCategory(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(defaultValue = "MONTHLY") BonusType type,
            RedirectAttributes ra) {
        try {
            bonusService.updateCategory(id, name, type);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("bonusError", e.getMessage());
        }
        return "redirect:/api/bonuses";
    }

    @PostMapping("/api/bonus-categories/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','SYS_ADMIN')")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bonusService.deleteCategory(id);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("bonusError", e.getMessage());
        }
        return "redirect:/api/bonuses";
    }

    @PostMapping("/api/bonuses/quarterly/distribute")
    @PreAuthorize("hasRole('ADMIN')")
    public String distributeQuarterly(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Set<Long> userIds,
            Authentication auth,
            RedirectAttributes ra) {
        try {
            List<Bonus> bonuses =
                    bonusService.distributeQuarterly(
                            year, quarter, categoryId, userIds, auth.getName());
            BigDecimal distributed =
                    bonuses.stream().map(Bonus::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            ra.addFlashAttribute("quarterlySuccessTitle", "Квартальний бонус розподілено");
            ra.addFlashAttribute(
                    "quarterlySuccessMessage",
                    "Розподілено "
                            + distributed
                            + " між "
                            + bonuses.size()
                            + " користувачами. Для перевірки перейдіть у «Фінансовий підсумок».");
            ra.addFlashAttribute("quarterlyShowSummaryLink", true);
            ra.addFlashAttribute("distributedQuarterYear", year);
            ra.addFlashAttribute("distributedQuarterStartMonth", (quarter - 1) * 3 + 1);
        } catch (IllegalArgumentException exception) {
            ra.addFlashAttribute("bonusError", exception.getMessage());
        }
        return "redirect:/api/bonuses";
    }

    @GetMapping("/api/bonuses/quarterly/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Object> quarterlySummary(@RequestParam int year, @RequestParam int quarter) {
        List<Bonus> distribution = bonusService.findQuarterlyDistribution(year, quarter);
        BigDecimal distributed =
                distribution.stream()
                        .map(Bonus::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("pool", bonusService.quarterlyPool(year, quarter));
        result.put("distributed", distributed);
        result.put("recipientCount", distribution.size());
        result.put("isDistributed", !distribution.isEmpty());
        return result;
    }

    @PostMapping("/api/bonuses/quarterly/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetQuarterly(
            @RequestParam int year,
            @RequestParam int quarter,
            Authentication auth,
            RedirectAttributes ra) {
        try {
            int removed = bonusService.resetQuarterlyDistribution(year, quarter, auth.getName());
            ra.addFlashAttribute("quarterlySuccessTitle", "Розподіл квартального бонусу скинуто");
            ra.addFlashAttribute(
                    "quarterlySuccessMessage",
                    "Видалено "
                            + removed
                            + " нарахувань за Q"
                            + quarter
                            + " "
                            + year
                            + ". KPI-сума знову доступна для розподілу.");
        } catch (IllegalArgumentException exception) {
            ra.addFlashAttribute("bonusError", exception.getMessage());
        }
        return "redirect:/api/bonuses";
    }

    private String redirect(String returnTo) {
        if ("summary".equals(returnTo))
            return "redirect:/api/overtime/review?mode=division&view=summary";
        return "review".equals(returnTo)
                ? "redirect:/api/overtime/review?mode=division"
                : "redirect:/api/bonuses";
    }

    private boolean hasProjectManagerTag(User user) {
        return !user.getTags().contains(BusinessTag.PROJECT_MANAGER_LEAD)
                && (user.getTags().contains(BusinessTag.PROJECT_MANAGER)
                        || (user.getDivision() != null
                                && user.getDivision()
                                        .getTags()
                                        .contains(BusinessTag.PROJECT_MANAGER)));
    }

    private void assertCanApproveBonuses(User user) {
        if (!user.getRoles().contains(Role.ADMIN)
                && !user.getRoles().contains(Role.MANAGER)
                && !user.getTags().contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
            throw new AccessDeniedException("Погоджувати бонуси може ADMIN, MANAGER або PM LEAD");
        }
    }

    private void assertCanOpenBonusModule(User user) {
        if (!user.getRoles().contains(Role.ADMIN)
                && !user.getTags().contains(BusinessTag.SYS_ADMIN)) {
            throw new AccessDeniedException("Модуль керування бонусами доступний лише ADMIN");
        }
    }
}
