package example.timeflows.controller;

import example.timeflows.controller.dto.CalendarDay;
import example.timeflows.controller.dto.MonthOption;
import example.timeflows.controller.dto.PasswordChangeRequest;
import example.timeflows.controller.dto.ProfileRequest;
import example.timeflows.exception.UserException;
import example.timeflows.model.Overtime;
import example.timeflows.model.User;
import example.timeflows.service.OvertimeService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class EmployeePageController {

    private final OvertimeService overtimeService;
    private final UserService userService;

    public EmployeePageController(OvertimeService overtimeService, UserService userService) {
        this.overtimeService = overtimeService;
        this.userService = userService;
    }

    @GetMapping("/api/overtime")
    public String overtime(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication,
            Model model
    ) {
        YearMonth selectedMonth = year == null || month == null
                ? YearMonth.now()
                : YearMonth.of(year, month);
        User currentUser = userService.findByEmail(authentication.getName());
        List<Overtime> overtimes = overtimeService.findMonth(currentUser.getEmail(), selectedMonth);
        Map<LocalDate, Overtime> overtimeByDate = overtimes.stream()
                .collect(Collectors.toMap(Overtime::getWorkDate, Function.identity()));

        model.addAttribute("calendarDays", buildCalendar(selectedMonth, overtimeByDate));
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("months", monthOptions());
        model.addAttribute("years", IntStream.rangeClosed(YearMonth.now().getYear() - 3, YearMonth.now().getYear() + 2).boxed().toList());
        model.addAttribute("activePage", "overtime");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("privilegedCurrentMonth", selectedMonth.equals(YearMonth.now()) &&
                (currentUser.getRoles().contains(example.timeflows.model.Role.ADMIN) || currentUser.getRoles().contains(example.timeflows.model.Role.MANAGER)));
        return "employee/overtime";
    }

    @GetMapping("/api/settings")
    public String settings(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName());
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setFirstName(user.getFirstName());
        profileRequest.setLastName(user.getLastName());

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("profileRequest", profileRequest);
        model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
        model.addAttribute("activePage", "settings");
        return "employee/settings";
    }

    @PostMapping("/api/settings/profile")
    public String updateProfile(
            @Valid @ModelAttribute ProfileRequest profileRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateSettings(authentication, model, profileRequest, new PasswordChangeRequest());
            return "employee/settings";
        }
        userService.updateProfile(authentication.getName(), profileRequest.getFirstName(), profileRequest.getLastName());
        return "redirect:/api/settings?profileUpdated";
    }

    @PostMapping("/api/settings/password")
    public String changePassword(
            @Valid @ModelAttribute PasswordChangeRequest passwordChangeRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model
    ) {
        ProfileRequest profileRequest = new ProfileRequest();
        if (bindingResult.hasErrors()) {
            populateSettings(authentication, model, profileRequest, passwordChangeRequest);
            return "employee/settings";
        }
        try {
            userService.changePassword(
                    authentication.getName(),
                    passwordChangeRequest.getCurrentPassword(),
                    passwordChangeRequest.getNewPassword(),
                    passwordChangeRequest.getConfirmPassword()
            );
            return "redirect:/api/settings?passwordUpdated";
        } catch (UserException exception) {
            populateSettings(authentication, model, profileRequest, passwordChangeRequest);
            model.addAttribute("passwordError", exception.getMessage());
            return "employee/settings";
        }
    }

    private void populateSettings(
            Authentication authentication,
            Model model,
            ProfileRequest profileRequest,
            PasswordChangeRequest passwordChangeRequest
    ) {
        User user = userService.findByEmail(authentication.getName());
        if (profileRequest.getFirstName() == null && profileRequest.getLastName() == null) {
            profileRequest.setFirstName(user.getFirstName());
            profileRequest.setLastName(user.getLastName());
        }
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("profileRequest", profileRequest);
        model.addAttribute("passwordChangeRequest", passwordChangeRequest);
        model.addAttribute("activePage", "settings");
    }

    private List<CalendarDay> buildCalendar(YearMonth selectedMonth, Map<LocalDate, Overtime> overtimeByDate) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate firstOfMonth = selectedMonth.atDay(1);
        LocalDate cursor = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate endOfMonth = selectedMonth.atEndOfMonth();
        LocalDate lastVisible = endOfMonth.plusDays(DayOfWeek.SUNDAY.getValue() - endOfMonth.getDayOfWeek().getValue());

        while (!cursor.isAfter(lastVisible)) {
            days.add(new CalendarDay(
                    cursor,
                    cursor.getDayOfMonth(),
                    YearMonth.from(cursor).equals(selectedMonth),
                    overtimeByDate.get(cursor),
                    resolveCssClass(cursor, selectedMonth, overtimeByDate.get(cursor))
            ));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private String resolveCssClass(LocalDate date, YearMonth selectedMonth, Overtime overtime) {
        if (!YearMonth.from(date).equals(selectedMonth)) {
            return "muted-day";
        }
        if (overtime == null) {
            return "";
        }
        return "has-overtime status-" + overtime.getStatus().name().toLowerCase();
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
