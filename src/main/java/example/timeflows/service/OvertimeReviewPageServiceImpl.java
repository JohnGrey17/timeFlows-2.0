package example.timeflows.service;

import example.timeflows.model.Bonus;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.Overtime;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeReviewPageServiceImpl implements OvertimeReviewPageService {
    private final OvertimeService overtimeService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final BonusService bonusService;
    private final OvertimeViewService overtimeViewService;

    public OvertimeReviewPageServiceImpl(
            OvertimeService overtimeService,
            UserService userService,
            DepartmentService departmentService,
            DivisionService divisionService,
            BonusService bonusService,
            OvertimeViewService overtimeViewService) {
        this.overtimeService = overtimeService;
        this.userService = userService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.bonusService = bonusService;
        this.overtimeViewService = overtimeViewService;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildPage(
            String email,
            String view,
            Long departmentId,
            Long divisionId,
            Integer year,
            Integer month,
            Long openBonusUserId) {
        User current = userService.findByEmail(email);
        boolean admin = current.getRoles().contains(Role.ADMIN);
        YearMonth selected = overtimeViewService.resolveMonth(year, month);
        Long effectiveDepartment =
                admin
                        ? (departmentId != null
                                ? departmentId
                                : current.getDivision().getDepartment().getId())
                        : current.getDivision().getDepartment().getId();
        Long effectiveDivision = admin ? divisionId : current.getDivision().getId();
        List<User> users =
                effectiveDivision != null
                        ? userService.findActiveUsersByDivision(effectiveDivision)
                        : userService.findActiveUsersByDepartment(effectiveDepartment);
        Long selectedUserId = users.isEmpty() ? null : users.get(0).getId();
        User selectedUser =
                users.stream()
                        .filter(user -> user.getId().equals(selectedUserId))
                        .findFirst()
                        .orElse(null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentUser", current);
        data.put(
                "departments",
                admin
                        ? departmentService.findAll()
                        : List.of(current.getDivision().getDepartment()));
        data.put(
                "divisions",
                admin
                        ? divisionService.findByDepartment(effectiveDepartment)
                        : List.of(current.getDivision()));
        data.put("users", users);
        data.put(
                "selectedDivision",
                effectiveDivision == null ? null : divisionService.findById(effectiveDivision));
        data.put("selectedDivisionId", effectiveDivision);
        data.put("selectedDepartmentId", effectiveDepartment);
        data.put("selectedUserId", selectedUserId);
        data.put("selectedUserDisplay", displayName(selectedUser));
        data.put("selectedMonth", selected);
        data.put("months", overtimeViewService.monthOptions());
        data.put("years", overtimeViewService.years());
        data.put("mode", "division");
        data.put("viewMode", "summary".equals(view) ? "summary" : "matrix");
        data.put("activePage", "review");
        data.put("admin", admin);
        data.put("categories", bonusService.findCategories());
        data.put("openBonusUserId", openBonusUserId);
        addDivisionData(data, users, effectiveDepartment, effectiveDivision, selected);
        return data;
    }

    private void addDivisionData(
            Map<String, Object> data,
            List<User> users,
            Long departmentId,
            Long divisionId,
            YearMonth month) {
        if (departmentId == null) return;
        List<Overtime> overtimes =
                divisionId != null
                        ? overtimeService.findDivisionMonth(divisionId, month)
                        : overtimeService.findDepartmentMonth(departmentId, month);
        Map<Long, Map<LocalDate, Overtime>> byUser =
                overtimes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        overtime -> overtime.getUser().getId(),
                                        Collectors.toMap(
                                                Overtime::getWorkDate, Function.identity())));
        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        List<Bonus> bonuses =
                bonusService.findMonth(month).stream()
                        .filter(bonus -> userIds.contains(bonus.getUser().getId()))
                        .toList();
        Map<Long, List<Bonus>> bonusesByUser =
                bonuses.stream().collect(Collectors.groupingBy(bonus -> bonus.getUser().getId()));
        Map<Long, BigDecimal> approvedTotals =
                bonuses.stream()
                        .filter(bonus -> bonus.getStatus() == BonusStatus.APPROVED)
                        .collect(
                                Collectors.groupingBy(
                                        bonus -> bonus.getUser().getId(),
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                Bonus::getAmount,
                                                BigDecimal::add)));
        data.put("dayHeaders", overtimeViewService.dayHeaders(month));
        data.put("bonusesByUser", bonusesByUser);
        data.put("categoryTotalsByUser", categoryTotals(users, bonusesByUser));
        data.put(
                "pendingBonusCounts",
                bonusesByUser.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry ->
                                                entry.getValue().stream()
                                                        .filter(
                                                                b ->
                                                                        b.getStatus()
                                                                                == BonusStatus
                                                                                        .PENDING)
                                                        .count())));
        data.put(
                "divisionRows",
                users.stream()
                        .map(
                                user ->
                                        overtimeViewService.paymentRow(
                                                user,
                                                month,
                                                byUser.getOrDefault(user.getId(), Map.of()),
                                                approvedTotals.getOrDefault(
                                                        user.getId(), BigDecimal.ZERO)))
                        .toList());
    }

    private Map<Long, Map<Long, BigDecimal>> categoryTotals(
            List<User> users, Map<Long, List<Bonus>> bonusesByUser) {
        var categories = bonusService.findCategories();
        return users.stream()
                .collect(
                        Collectors.toMap(
                                User::getId,
                                user ->
                                        categories.stream()
                                                .collect(
                                                        Collectors.toMap(
                                                                category -> category.getId(),
                                                                category ->
                                                                        bonusesByUser
                                                                                .getOrDefault(
                                                                                        user
                                                                                                .getId(),
                                                                                        List.of())
                                                                                .stream()
                                                                                .filter(
                                                                                        bonus ->
                                                                                                bonus
                                                                                                                .getStatus()
                                                                                                        == BonusStatus
                                                                                                                .APPROVED)
                                                                                .filter(
                                                                                        bonus ->
                                                                                                bonus.getCategory()
                                                                                                        .getId()
                                                                                                        .equals(
                                                                                                                category
                                                                                                                        .getId()))
                                                                                .map(
                                                                                        Bonus
                                                                                                ::getAmount)
                                                                                .reduce(
                                                                                        BigDecimal
                                                                                                .ZERO,
                                                                                        BigDecimal
                                                                                                ::add)))));
    }

    private String displayName(User user) {
        if (user == null) return "";
        return ((user.getLastName() == null ? "" : user.getLastName())
                        + " "
                        + (user.getFirstName() == null ? "" : user.getFirstName())
                        + " — "
                        + user.getEmail())
                .trim();
    }
}
