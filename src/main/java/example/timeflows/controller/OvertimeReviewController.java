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

    public OvertimeReviewController(
            OvertimeService overtimeService,
            UserService userService,
            DepartmentService departmentService,
            DivisionService divisionService
    ) {
        this.overtimeService = overtimeService;
        this.userService = userService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
    }

    @GetMapping("/api/overtime/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String review(
            @RequestParam(defaultValue = "division") String mode,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication,
            Model model
    ) {
        User currentUser = userService.findByEmail(authentication.getName());
        boolean admin = currentUser.getRoles().contains(Role.ADMIN);
        YearMonth selectedMonth = year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
        Long effectiveDivisionId = admin ? divisionId : currentUser.getDivision().getId();
        if (effectiveDivisionId == null && !divisionService.findAll().isEmpty()) {
            effectiveDivisionId = divisionService.findAll().get(0).getId();
        }

        List<User> users = effectiveDivisionId == null ? userService.findActiveUsers() : userService.findActiveUsersByDivision(effectiveDivisionId);
        Long effectiveUserId = userId != null ? userId : (users.isEmpty() ? null : users.get(0).getId());
        Division selectedDivision = effectiveDivisionId == null ? null : divisionService.findById(effectiveDivisionId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("departments", admin ? departmentService.findAll() : List.of(currentUser.getDivision().getDepartment()));
        model.addAttribute("divisions", admin ? divisionService.findAll() : List.of(currentUser.getDivision()));
        model.addAttribute("users", users);
        model.addAttribute("selectedDivision", selectedDivision);
        model.addAttribute("selectedDivisionId", effectiveDivisionId);
        model.addAttribute("selectedUserId", effectiveUserId);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("months", monthOptions());
        model.addAttribute("years", IntStream.rangeClosed(YearMonth.now().getYear() - 3, YearMonth.now().getYear() + 2).boxed().toList());
        model.addAttribute("mode", mode);
        model.addAttribute("activePage", "review");

        if ("employee".equals(mode) && effectiveUserId != null) {
            Map<LocalDate, Overtime> overtimeByDate = overtimeService.findUserMonth(effectiveUserId, selectedMonth)
                    .stream()
                    .collect(Collectors.toMap(Overtime::getWorkDate, Function.identity()));
            model.addAttribute("calendarDays", buildCalendar(selectedMonth, overtimeByDate));
        } else if (effectiveDivisionId != null) {
            Map<Long, Map<LocalDate, Overtime>> byUser = overtimeService.findDivisionMonth(effectiveDivisionId, selectedMonth)
                    .stream()
                    .collect(Collectors.groupingBy(overtime -> overtime.getUser().getId(), Collectors.toMap(Overtime::getWorkDate, Function.identity())));
            model.addAttribute("divisionDays", selectedMonth.lengthOfMonth());
            model.addAttribute("divisionRows", users.stream()
                    .map(user -> new DivisionOvertimeRow(user, buildMonthDays(selectedMonth, byUser.getOrDefault(user.getId(), Map.of()))))
                    .toList());
        }

        return "manager/overtime-review";
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
}
