package example.timeflows.service;

import example.timeflows.controller.dto.CalendarDay;
import example.timeflows.controller.dto.DayHeader;
import example.timeflows.controller.dto.DivisionOvertimeRow;
import example.timeflows.controller.dto.MonthOption;
import example.timeflows.model.Overtime;
import example.timeflows.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface OvertimeViewService {
    YearMonth resolveMonth(Integer year, Integer month);

    List<Integer> years();

    List<MonthOption> monthOptions();

    List<CalendarDay> buildCalendar(
            YearMonth selectedMonth, Map<LocalDate, Overtime> overtimeByDate);

    List<DayHeader> dayHeaders(YearMonth month);

    DivisionOvertimeRow paymentRow(
            User user, YearMonth month, Map<LocalDate, Overtime> byDate, BigDecimal bonuses);
}
