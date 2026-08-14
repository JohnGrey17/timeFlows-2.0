package example.timeflows.controller;

import example.timeflows.controller.dto.CalendarDay;
import example.timeflows.controller.dto.DivisionOvertimeRow;
import example.timeflows.controller.dto.MonthOption;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.exception.UserException;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.OvertimeService;
import example.timeflows.service.UserService;
import example.timeflows.service.BonusService;
import example.timeflows.model.Bonus;
import example.timeflows.controller.dto.DayHeader;
import example.timeflows.controller.dto.OvertimePaymentDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class OvertimeReviewController {

    private final OvertimeService overtimeService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final BonusService bonusService;

    public OvertimeReviewController(
            OvertimeService overtimeService,
            UserService userService,
            DepartmentService departmentService,
            DivisionService divisionService, BonusService bonusService
    ) {
        this.overtimeService = overtimeService;
        this.userService = userService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.bonusService = bonusService;
    }

    @GetMapping("/api/overtime/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String review(
            @RequestParam(defaultValue = "division") String mode,
            @RequestParam(defaultValue = "matrix") String view,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long openBonusUserId,
            Authentication authentication,
            Model model
    ) {
        mode = "division";
        view = "summary".equals(view) ? "summary" : "matrix";
        userId = null;
        User currentUser = userService.findByEmail(authentication.getName());
        boolean admin = currentUser.getRoles().contains(Role.ADMIN);
        YearMonth selectedMonth = year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
        Long effectiveDepartmentId = admin
                ? (departmentId != null ? departmentId : currentUser.getDivision().getDepartment().getId())
                : currentUser.getDivision().getDepartment().getId();
        Long effectiveDivisionId = admin ? divisionId : currentUser.getDivision().getId();

        List<User> users = effectiveDivisionId != null
                ? userService.findActiveUsersByDivision(effectiveDivisionId)
                : userService.findActiveUsersByDepartment(effectiveDepartmentId);
        Long effectiveUserId = userId != null ? userId : (users.isEmpty() ? null : users.get(0).getId());
        Division selectedDivision = effectiveDivisionId == null ? null : divisionService.findById(effectiveDivisionId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("departments", admin ? departmentService.findAll() : List.of(currentUser.getDivision().getDepartment()));
        model.addAttribute("divisions", admin ? divisionService.findByDepartment(effectiveDepartmentId) : List.of(currentUser.getDivision()));
        model.addAttribute("users", users);
        model.addAttribute("selectedDivision", selectedDivision);
        model.addAttribute("selectedDivisionId", effectiveDivisionId);
        model.addAttribute("selectedDepartmentId", effectiveDepartmentId);
        model.addAttribute("selectedUserId", effectiveUserId);
        User selectedUser = users.stream().filter(user -> user.getId().equals(effectiveUserId)).findFirst().orElse(null);
        model.addAttribute("selectedUserDisplay", selectedUser == null ? "" :
                ((selectedUser.getLastName() == null ? "" : selectedUser.getLastName()) + " " +
                        (selectedUser.getFirstName() == null ? "" : selectedUser.getFirstName()) + " — " + selectedUser.getEmail()).trim());
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("months", monthOptions());
        model.addAttribute("years", IntStream.rangeClosed(YearMonth.now().getYear() - 3, YearMonth.now().getYear() + 2).boxed().toList());
        model.addAttribute("mode", mode);
        model.addAttribute("viewMode", view);
        model.addAttribute("activePage", "review");
        model.addAttribute("admin", admin);
        model.addAttribute("categories", bonusService.findCategories());
        model.addAttribute("openBonusUserId", openBonusUserId);

        if ("employee".equals(mode) && effectiveUserId != null) {
            Map<LocalDate, Overtime> overtimeByDate = overtimeService.findUserMonth(effectiveUserId, selectedMonth)
                    .stream()
                    .collect(Collectors.toMap(Overtime::getWorkDate, Function.identity()));
            model.addAttribute("calendarDays", buildCalendar(selectedMonth, overtimeByDate));
            model.addAttribute("employeeBonuses", bonusService.findUserMonth(effectiveUserId, selectedMonth));
        } else if (effectiveDepartmentId != null) {
            List<Overtime> selectedOvertimes = effectiveDivisionId != null
                    ? overtimeService.findDivisionMonth(effectiveDivisionId, selectedMonth)
                    : overtimeService.findDepartmentMonth(effectiveDepartmentId, selectedMonth);
            Map<Long, Map<LocalDate, Overtime>> byUser = selectedOvertimes
                    .stream()
                    .collect(Collectors.groupingBy(overtime -> overtime.getUser().getId(), Collectors.toMap(Overtime::getWorkDate, Function.identity())));
            model.addAttribute("dayHeaders", dayHeaders(selectedMonth));
            Map<Long, BigDecimal> bonusTotals = bonusService.findMonth(selectedMonth).stream()
                    .filter(b -> users.stream().anyMatch(u -> u.getId().equals(b.getUser().getId())))
                    .filter(b -> b.getStatus() == example.timeflows.model.BonusStatus.APPROVED)
                    .collect(Collectors.groupingBy(b -> b.getUser().getId(), Collectors.reducing(BigDecimal.ZERO, Bonus::getAmount, BigDecimal::add)));
            Map<Long, List<Bonus>> bonusesByUser = bonusService.findMonth(selectedMonth).stream()
                    .filter(b -> users.stream().anyMatch(u -> u.getId().equals(b.getUser().getId())))
                    .collect(Collectors.groupingBy(b -> b.getUser().getId()));
            model.addAttribute("bonusesByUser", bonusesByUser);
            Map<Long, Map<Long, BigDecimal>> categoryTotalsByUser = users.stream().collect(Collectors.toMap(
                    User::getId,
                    user -> bonusService.findCategories().stream().collect(Collectors.toMap(
                            category -> category.getId(),
                            category -> bonusesByUser.getOrDefault(user.getId(), List.of()).stream()
                                    .filter(b -> b.getStatus() == example.timeflows.model.BonusStatus.APPROVED)
                                    .filter(b -> b.getCategory().getId().equals(category.getId()))
                                    .map(Bonus::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    ))
            ));
            model.addAttribute("categoryTotalsByUser", categoryTotalsByUser);
            model.addAttribute("pendingBonusCounts", bonusesByUser.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().filter(b -> b.getStatus() == example.timeflows.model.BonusStatus.PENDING).count())));
            int workingHours = workingHours(selectedMonth);
            model.addAttribute("divisionRows", users.stream()
                    .map(user -> paymentRow(user, selectedMonth, byUser.getOrDefault(user.getId(), Map.of()),
                            bonusTotals.getOrDefault(user.getId(), BigDecimal.ZERO), workingHours))
                    .toList());
        }

        return "manager/overtime-review";
    }

    private DivisionOvertimeRow paymentRow(User user, YearMonth month, Map<LocalDate, Overtime> byDate,
                                            BigDecimal bonuses, int workingHours) {
        BigDecimal salary = user.getSalary() == null ? BigDecimal.ZERO : user.getSalary();
        BigDecimal approvedHours = byDate.values().stream()
                .filter(o -> o.getStatus() == example.timeflows.model.OvertimeStatus.APPROVED)
                .map(o -> BigDecimal.valueOf(o.getHours()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overtimeAmount = workingHours == 0 ? BigDecimal.ZERO : salary
                .divide(BigDecimal.valueOf(workingHours), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(2)).multiply(approvedHours).setScale(2, RoundingMode.HALF_UP);
        BigDecimal overtimeRate = workingHours == 0 ? BigDecimal.ZERO : salary
                .divide(BigDecimal.valueOf(workingHours), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(2));
        List<OvertimePaymentDetail> overtimeDetails = byDate.values().stream()
                .filter(o -> o.getStatus() == example.timeflows.model.OvertimeStatus.APPROVED)
                .sorted(java.util.Comparator.comparing(Overtime::getWorkDate))
                .map(o -> new OvertimePaymentDetail(o.getWorkDate(), o.getHours(), o.getDescription(),
                        overtimeRate.multiply(BigDecimal.valueOf(o.getHours())).setScale(2, RoundingMode.HALF_UP)))
                .toList();
        return new DivisionOvertimeRow(user, buildMonthDays(month, byDate), bonuses, salary,
                approvedHours, overtimeAmount, salary.add(overtimeAmount).add(bonuses).setScale(2, RoundingMode.HALF_UP), overtimeDetails);
    }

    private int workingHours(YearMonth month) {
        return (int) month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count() * 8;
    }

    @PostMapping("/api/overtime/review/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String approve(
            @RequestParam Long overtimeId,
            @RequestParam(required = false) String comment,
            Authentication authentication
    ) {
        assertCanReview(overtimeId, authentication);
        overtimeService.approve(overtimeId, comment);
        return "redirect:/api/overtime/review";
    }

    @PostMapping("/api/overtime/review/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String reject(@RequestParam Long overtimeId, @RequestParam String comment, Authentication authentication) {
        assertCanReview(overtimeId, authentication);
        overtimeService.reject(overtimeId, comment);
        return "redirect:/api/overtime/review";
    }

    private void assertCanReview(Long overtimeId, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            return;
        }
        Overtime overtime = overtimeService.findById(overtimeId);
        if (!overtime.getUser().getDivision().getId().equals(currentUser.getDivision().getId())) {
            throw new UserException("Керівник може переглядати тільки overtime свого відділу");
        }
    }

    private List<CalendarDay> buildCalendar(YearMonth selectedMonth, Map<LocalDate, Overtime> overtimeByDate) {
        LocalDate first = selectedMonth.atDay(1);
        LocalDate cursor = first.minusDays(first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate end = selectedMonth.atEndOfMonth();
        LocalDate last = end.plusDays(DayOfWeek.SUNDAY.getValue() - end.getDayOfWeek().getValue());
        java.util.ArrayList<CalendarDay> days = new java.util.ArrayList<>();
        while (!cursor.isAfter(last)) {
            Overtime overtime = overtimeByDate.get(cursor);
            days.add(new CalendarDay(cursor, cursor.getDayOfMonth(), YearMonth.from(cursor).equals(selectedMonth), overtime, css(cursor, selectedMonth, overtime)));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private List<CalendarDay> buildMonthDays(YearMonth selectedMonth, Map<LocalDate, Overtime> overtimeByDate) {
        return IntStream.rangeClosed(1, selectedMonth.lengthOfMonth())
                .mapToObj(day -> {
                    LocalDate date = selectedMonth.atDay(day);
                    Overtime overtime = overtimeByDate.get(date);
                    return new CalendarDay(date, day, true, overtime, css(date, selectedMonth, overtime));
                })
                .toList();
    }

    private String css(LocalDate date, YearMonth month, Overtime overtime) {
        if (!YearMonth.from(date).equals(month)) {
            return "muted-day";
        }
        return overtime == null ? "" : "has-overtime status-" + overtime.getStatus().name().toLowerCase();
    }

    private List<MonthOption> monthOptions() {
        return List.of(
                new MonthOption(1, "Січень"),
                new MonthOption(2, "Лютий"),
                new MonthOption(3, "Березень"),
                new MonthOption(4, "Квітень"),
                new MonthOption(5, "Травень"),
                new MonthOption(6, "Червень"),
                new MonthOption(7, "Липень"),
                new MonthOption(8, "Серпень"),
                new MonthOption(9, "Вересень"),
                new MonthOption(10, "Жовтень"),
                new MonthOption(11, "Листопад"),
                new MonthOption(12, "Грудень")
        );
    }

    private List<DayHeader> dayHeaders(YearMonth month) {
        List<String> weekdays = List.of("пн", "вт", "ср", "чт", "пт", "сб", "нд");
        return IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(day -> new DayHeader(day, weekdays.get(month.atDay(day).getDayOfWeek().getValue() - 1)))
                .toList();
    }
}
