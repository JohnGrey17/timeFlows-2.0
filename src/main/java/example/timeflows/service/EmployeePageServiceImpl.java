package example.timeflows.service;

import example.timeflows.controller.dto.PasswordChangeRequest;
import example.timeflows.controller.dto.ProfileRequest;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.model.Overtime;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmployeePageServiceImpl implements EmployeePageService {
    private final OvertimeService overtimeService;
    private final UserService userService;
    private final OvertimeViewService overtimeViewService;
    private final TimeflowsMapper mapper;

    public EmployeePageServiceImpl(
            OvertimeService overtimeService,
            UserService userService,
            OvertimeViewService overtimeViewService,
            TimeflowsMapper mapper) {
        this.overtimeService = overtimeService;
        this.userService = userService;
        this.overtimeViewService = overtimeViewService;
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> overtimePage(String email, Integer year, Integer month) {
        YearMonth selected = overtimeViewService.resolveMonth(year, month);
        User user = userService.findByEmail(email);
        Map<java.time.LocalDate, Overtime> byDate =
                overtimeService.findMonth(email, selected).stream()
                        .collect(Collectors.toMap(Overtime::getWorkDate, Function.identity()));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("calendarDays", overtimeViewService.buildCalendar(selected, byDate));
        data.put("selectedMonth", selected);
        data.put("months", overtimeViewService.monthOptions());
        data.put("years", overtimeViewService.years());
        data.put("activePage", "overtime");
        data.put("currentUser", user);
        data.put(
                "privilegedCurrentMonth",
                selected.equals(YearMonth.now())
                        && (user.getRoles().contains(Role.ADMIN)
                                || user.getRoles().contains(Role.MANAGER)));
        return data;
    }

    @Override
    public Map<String, Object> settingsPage(
            String email, ProfileRequest profile, PasswordChangeRequest password) {
        User user = userService.findByEmail(email);
        if (profile == null) profile = mapper.toProfileRequest(user);
        if (profile.getFirstName() == null && profile.getLastName() == null)
            profile = mapper.toProfileRequest(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", user);
        data.put("currentUser", user);
        data.put("profileRequest", profile);
        data.put(
                "passwordChangeRequest", password == null ? new PasswordChangeRequest() : password);
        data.put("activePage", "settings");
        return data;
    }
}
