package example.timeflows.service;

import example.timeflows.model.Bonus;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
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
    private final DirectorateService directorateService;
    private final DivisionService divisionService;
    private final SubdivisionService subdivisionService;
    private final BonusService bonusService;
    private final OvertimeViewService overtimeViewService;

    public OvertimeReviewPageServiceImpl(
            OvertimeService overtimeService,
            UserService userService,
            DepartmentService departmentService,
            DirectorateService directorateService,
            DivisionService divisionService,
            SubdivisionService subdivisionService,
            BonusService bonusService,
            OvertimeViewService overtimeViewService) {
        this.overtimeService = overtimeService;
        this.userService = userService;
        this.departmentService = departmentService;
        this.directorateService = directorateService;
        this.divisionService = divisionService;
        this.subdivisionService = subdivisionService;
        this.bonusService = bonusService;
        this.overtimeViewService = overtimeViewService;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildPage(
            String email,
            String mode,
            String view,
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            OvertimeStatus status,
            Integer year,
            Integer month,
            Long userId,
            Long openBonusUserId) {
        User current = userService.findByEmail(email);
        boolean admin = current.getRoles().contains(Role.ADMIN);
        YearMonth selected = overtimeViewService.resolveMonth(year, month);
        boolean employeeMode = "employee".equals(mode) && userId != null;
        User requestedUser = employeeMode ? userService.findById(userId) : null;
        if (requestedUser != null
                && !admin
                && !requestedUser.getDivision().getId().equals(current.getDivision().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Немає доступу до перепрацювань цього користувача");
        }
        Long effectiveDepartment =
                requestedUser != null
                        ? requestedUser.getDivision().getDepartment().getId()
                        : admin
                                ? (departmentId != null
                                        ? departmentId
                                        : current.getDivision().getDepartment().getId())
                                : current.getDivision().getDepartment().getId();
        Long effectiveDivision =
                requestedUser != null
                        ? requestedUser.getDivision().getId()
                        : admin ? divisionId : current.getDivision().getId();
        Long effectiveDirectorate =
                requestedUser != null && requestedUser.getDivision().getDirectorate() != null
                        ? requestedUser.getDivision().getDirectorate().getId()
                        : admin ? directorateId : null;
        Long effectiveSubdivision =
                requestedUser != null && requestedUser.getSubdivision() != null
                        ? requestedUser.getSubdivision().getId()
                        : admin ? subdivisionId : null;
        List<User> users =
                requestedUser != null
                        ? List.of(requestedUser)
                        : effectiveSubdivision != null
                                ? userService.findActiveUsersBySubdivision(effectiveSubdivision)
                                : effectiveDivision != null
                                        ? userService.findActiveUsersByDivision(effectiveDivision)
                                        : effectiveDirectorate != null
                                                ? userService.findActiveUsersByDirectorate(
                                                        effectiveDirectorate)
                                                : userService.findActiveUsersByDepartment(
                                                        effectiveDepartment);
        Long selectedUserId =
                requestedUser != null
                        ? requestedUser.getId()
                        : users.isEmpty() ? null : users.get(0).getId();
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
                "directorates",
                admin ? directorateService.findByDepartment(effectiveDepartment) : List.of());
        data.put(
                "divisions",
                admin
                        ? (effectiveDirectorate == null
                                ? divisionService.findByDepartment(effectiveDepartment)
                                : divisionService.findByDirectorate(effectiveDirectorate))
                        : List.of(current.getDivision()));
        data.put(
                "subdivisions",
                admin
                        ? (effectiveDivision == null
                                ? List.of()
                                : subdivisionService.findByDivision(effectiveDivision))
                        : List.of());
        data.put("users", users);
        data.put(
                "selectedDivision",
                effectiveDivision == null ? null : divisionService.findById(effectiveDivision));
        data.put("selectedDivisionId", effectiveDivision);
        data.put("selectedDirectorateId", effectiveDirectorate);
        data.put("selectedSubdivisionId", effectiveSubdivision);
        data.put("selectedDepartmentId", effectiveDepartment);
        data.put("selectedStatus", status);
        data.put(
                "overtimeStatuses",
                admin
                        ? List.of(
                                OvertimeStatus.APPROVED_MANAGER,
                                OvertimeStatus.DECLINED,
                                OvertimeStatus.APPROVED_ADMIN)
                        : List.of(
                                OvertimeStatus.CHECKING,
                                OvertimeStatus.APPROVED_MANAGER,
                                OvertimeStatus.DECLINED));
        data.put("selectedUserId", selectedUserId);
        data.put("selectedUserDisplay", displayName(selectedUser));
        data.put("selectedMonth", selected);
        data.put("months", overtimeViewService.monthOptions());
        data.put("years", overtimeViewService.years());
        data.put("mode", employeeMode ? "employee" : "division");
        data.put("viewMode", "summary".equals(view) ? "summary" : "matrix");
        data.put("activePage", "review");
        data.put("admin", admin);
        data.put(
                "canApproveBonuses",
                admin
                        || current.getRoles().contains(Role.MANAGER)
                        || current.getTags()
                                .contains(
                                        example.timeflows.model.BusinessTag.PROJECT_MANAGER_LEAD));
        data.put(
                "canManageKpi",
                current.getTags()
                        .contains(example.timeflows.model.BusinessTag.PROJECT_MANAGER_LEAD));
        boolean projectManagerView =
                !users.isEmpty() && users.stream().allMatch(this::canReceiveKpi);
        boolean hasProjectManagers = users.stream().anyMatch(this::canReceiveKpi);
        data.put("projectManagerView", projectManagerView);
        data.put("hasProjectManagers", hasProjectManagers);
        data.put(
                "projectManagerUserIds",
                users.stream()
                        .filter(this::canReceiveKpi)
                        .map(User::getId)
                        .collect(Collectors.toSet()));
        data.put(
                "projectManagerLeadUserIds",
                users.stream()
                        .filter(
                                user ->
                                        user.getTags()
                                                .contains(
                                                        example.timeflows.model.BusinessTag
                                                                .PROJECT_MANAGER_LEAD))
                        .map(User::getId)
                        .collect(Collectors.toSet()));
        data.put("categories", projectManagerView ? List.of() : bonusService.findCategories());
        boolean canSeeQuarterKpiPool =
                admin
                        || current.getTags()
                                .contains(example.timeflows.model.BusinessTag.PROJECT_MANAGER_LEAD);
        int quarterStartMonth = ((selected.getMonthValue() - 1) / 3) * 3 + 1;
        BigDecimal currentQuarterKpiPool =
                canSeeQuarterKpiPool
                        ? java.util.stream.IntStream.range(0, 3)
                                .mapToObj(
                                        offset ->
                                                bonusService.findMonth(
                                                        YearMonth.of(
                                                                selected.getYear(),
                                                                Month.of(
                                                                        quarterStartMonth
                                                                                + offset))))
                                .flatMap(List::stream)
                                .filter(
                                        bonus ->
                                                bonus.getType()
                                                        == example.timeflows.model.BonusType.KPI)
                                .filter(bonus -> bonus.getStatus() == BonusStatus.APPROVED)
                                .map(Bonus::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;
        data.put("canSeeQuarterKpiPool", canSeeQuarterKpiPool);
        data.put("currentQuarterNumber", ((selected.getMonthValue() - 1) / 3) + 1);
        data.put("currentQuarterKpiPool", currentQuarterKpiPool);
        data.put("openBonusUserId", openBonusUserId);
        if (employeeMode) {
            Map<LocalDate, Overtime> byDate =
                    overtimeService.findUserMonth(selectedUserId, selected).stream()
                            .filter(overtime -> status == null || overtime.getStatus() == status)
                            .collect(Collectors.toMap(Overtime::getWorkDate, Function.identity()));
            data.put("calendarDays", overtimeViewService.buildCalendar(selected, byDate));
            data.put("employeeBonuses", bonusService.findUserMonth(selectedUserId, selected));
        }
        addDivisionData(
                data, users, effectiveDepartment, effectiveDivision, status, selected, admin);
        return data;
    }

    private void addDivisionData(
            Map<String, Object> data,
            List<User> users,
            Long departmentId,
            Long divisionId,
            OvertimeStatus status,
            YearMonth month,
            boolean admin) {
        if (departmentId == null) return;
        List<Overtime> overtimes =
                divisionId != null
                        ? overtimeService.findDivisionMonth(divisionId, month)
                        : overtimeService.findDepartmentMonth(departmentId, month);
        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        overtimes =
                overtimes.stream()
                        .filter(overtime -> userIds.contains(overtime.getUser().getId()))
                        .filter(
                                overtime ->
                                        !admin
                                                || overtime.getStatus()
                                                        == OvertimeStatus.APPROVED_MANAGER
                                                || overtime.getStatus()
                                                        == OvertimeStatus.APPROVED_ADMIN
                                                || overtime.getStatus() == OvertimeStatus.DECLINED)
                        .filter(overtime -> status == null || overtime.getStatus() == status)
                        .toList();
        data.put("filteredOvertimes", overtimes);
        data.put(
                "bulkApprovableCount",
                overtimes.stream().filter(overtimeService::canAdminApprove).count());
        Map<Long, Map<LocalDate, Overtime>> byUser =
                overtimes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        overtime -> overtime.getUser().getId(),
                                        Collectors.toMap(
                                                Overtime::getWorkDate, Function.identity())));
        data.put(
                "overviewHoursByUser",
                users.stream()
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        user ->
                                                byUser
                                                        .getOrDefault(user.getId(), Map.of())
                                                        .values()
                                                        .stream()
                                                        .filter(
                                                                overtime ->
                                                                        isFinalApproved(
                                                                                overtime
                                                                                        .getStatus()))
                                                        .map(
                                                                overtime ->
                                                                        BigDecimal.valueOf(
                                                                                overtime
                                                                                        .getHours()))
                                                        .reduce(
                                                                BigDecimal.ZERO,
                                                                BigDecimal::add))));
        data.put(
                "overviewOvertimesByUser",
                users.stream()
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        user ->
                                                byUser
                                                        .getOrDefault(user.getId(), Map.of())
                                                        .values()
                                                        .stream()
                                                        .filter(
                                                                overtime ->
                                                                        isFinalApproved(
                                                                                overtime
                                                                                        .getStatus()))
                                                        .sorted(
                                                                java.util.Comparator.comparing(
                                                                        Overtime::getWorkDate))
                                                        .toList())));
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
        data.put("categoryCountsByUser", categoryCounts(users, bonusesByUser));
        data.put("typeTotalsByUser", typeTotals(users, bonusesByUser));
        data.put("typeCountsByUser", typeCounts(users, bonusesByUser));
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

    private boolean isFinalApproved(OvertimeStatus status) {
        return status == OvertimeStatus.APPROVED_ADMIN || status == OvertimeStatus.APPROVED;
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
                                                                                                                .getCategory()
                                                                                                        != null)
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

    private Map<Long, Map<Long, Long>> categoryCounts(
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
                                                                                                                .getCategory()
                                                                                                        != null)
                                                                                .filter(
                                                                                        bonus ->
                                                                                                bonus.getCategory()
                                                                                                        .getId()
                                                                                                        .equals(
                                                                                                                category
                                                                                                                        .getId()))
                                                                                .count()))));
    }

    private Map<Long, Map<example.timeflows.model.BonusType, BigDecimal>> typeTotals(
            List<User> users, Map<Long, List<Bonus>> bonusesByUser) {
        Map<Long, Map<example.timeflows.model.BonusType, BigDecimal>> result =
                new LinkedHashMap<>();
        for (User user : users) {
            Map<example.timeflows.model.BonusType, BigDecimal> totals = new LinkedHashMap<>();
            for (var type : example.timeflows.model.BonusType.values()) {
                BigDecimal total =
                        bonusesByUser.getOrDefault(user.getId(), List.of()).stream()
                                .filter(bonus -> bonus.getType() == type)
                                .filter(bonus -> bonus.getStatus() == BonusStatus.APPROVED)
                                .map(Bonus::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                totals.put(type, total);
            }
            result.put(user.getId(), totals);
        }
        return result;
    }

    private Map<Long, Map<example.timeflows.model.BonusType, Long>> typeCounts(
            List<User> users, Map<Long, List<Bonus>> bonusesByUser) {
        Map<Long, Map<example.timeflows.model.BonusType, Long>> result = new LinkedHashMap<>();
        for (User user : users) {
            Map<example.timeflows.model.BonusType, Long> counts = new LinkedHashMap<>();
            for (var type : example.timeflows.model.BonusType.values()) {
                long count =
                        bonusesByUser.getOrDefault(user.getId(), List.of()).stream()
                                .filter(bonus -> bonus.getType() == type)
                                .count();
                counts.put(type, count);
            }
            result.put(user.getId(), counts);
        }
        return result;
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

    private boolean canReceiveKpi(User user) {
        return !user.getTags().contains(example.timeflows.model.BusinessTag.PROJECT_MANAGER_LEAD)
                && (user.getTags().contains(example.timeflows.model.BusinessTag.PROJECT_MANAGER)
                        || (user.getDivision() != null
                                && user.getDivision()
                                        .getTags()
                                        .contains(
                                                example.timeflows.model.BusinessTag
                                                        .PROJECT_MANAGER)));
    }
}
