package example.timeflows.service;

import example.timeflows.controller.dto.CalendarDay;
import example.timeflows.controller.dto.DayHeader;
import example.timeflows.controller.dto.DivisionOvertimeRow;
import example.timeflows.controller.dto.MonthOption;
import example.timeflows.controller.dto.OvertimePaymentDetail;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class OvertimeViewServiceImpl implements OvertimeViewService {
    @Override
    public YearMonth resolveMonth(Integer year, Integer month) {
        return year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
    }

    @Override
    public List<Integer> years() {
        return IntStream.rangeClosed(YearMonth.now().getYear() - 3, YearMonth.now().getYear() + 2)
                .boxed()
                .toList();
    }

    @Override
    public List<MonthOption> monthOptions() {
        return List.of(
                new MonthOption(1, "Січень"), new MonthOption(2, "Лютий"),
                new MonthOption(3, "Березень"), new MonthOption(4, "Квітень"),
                new MonthOption(5, "Травень"), new MonthOption(6, "Червень"),
                new MonthOption(7, "Липень"), new MonthOption(8, "Серпень"),
                new MonthOption(9, "Вересень"), new MonthOption(10, "Жовтень"),
                new MonthOption(11, "Листопад"), new MonthOption(12, "Грудень"));
    }

    @Override
    public List<CalendarDay> buildCalendar(YearMonth month, Map<LocalDate, Overtime> byDate) {
        LocalDate first = month.atDay(1);
        LocalDate cursor = first.minusDays(first.getDayOfWeek().getValue() - 1);
        LocalDate end = month.atEndOfMonth();
        LocalDate last = end.plusDays(7 - end.getDayOfWeek().getValue());
        List<CalendarDay> days = new ArrayList<>();
        while (!cursor.isAfter(last)) {
            Overtime overtime = byDate.get(cursor);
            days.add(
                    new CalendarDay(
                            cursor,
                            cursor.getDayOfMonth(),
                            YearMonth.from(cursor).equals(month),
                            overtime,
                            css(cursor, month, overtime)));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    @Override
    public List<DayHeader> dayHeaders(YearMonth month) {
        List<String> weekdays = List.of("пн", "вт", "ср", "чт", "пт", "сб", "нд");
        return IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(
                        day ->
                                new DayHeader(
                                        day,
                                        weekdays.get(
                                                month.atDay(day).getDayOfWeek().getValue() - 1)))
                .toList();
    }

    @Override
    public DivisionOvertimeRow paymentRow(
            User user, YearMonth month, Map<LocalDate, Overtime> byDate, BigDecimal bonuses) {
        BigDecimal salary = user.getSalary() == null ? BigDecimal.ZERO : user.getSalary();
        BigDecimal hours =
                byDate.values().stream()
                        .filter(o -> o.getStatus() == OvertimeStatus.APPROVED)
                        .map(o -> BigDecimal.valueOf(o.getHours()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        int workingHours =
                (int)
                                month.atDay(1)
                                        .datesUntil(month.plusMonths(1).atDay(1))
                                        .filter(
                                                d ->
                                                        d.getDayOfWeek() != DayOfWeek.SATURDAY
                                                                && d.getDayOfWeek()
                                                                        != DayOfWeek.SUNDAY)
                                        .count()
                        * 8;
        BigDecimal rate =
                workingHours == 0
                        ? BigDecimal.ZERO
                        : salary.divide(BigDecimal.valueOf(workingHours), 8, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(2));
        BigDecimal amount = rate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        List<OvertimePaymentDetail> details =
                byDate.values().stream()
                        .filter(o -> o.getStatus() == OvertimeStatus.APPROVED)
                        .sorted(Comparator.comparing(Overtime::getWorkDate))
                        .map(
                                o ->
                                        new OvertimePaymentDetail(
                                                o.getWorkDate(),
                                                o.getHours(),
                                                o.getDescription(),
                                                rate.multiply(BigDecimal.valueOf(o.getHours()))
                                                        .setScale(2, RoundingMode.HALF_UP)))
                        .toList();
        return new DivisionOvertimeRow(
                user,
                monthDays(month, byDate),
                bonuses,
                salary,
                hours,
                amount,
                salary.add(amount).add(bonuses).setScale(2, RoundingMode.HALF_UP),
                details);
    }

    private List<CalendarDay> monthDays(YearMonth month, Map<LocalDate, Overtime> byDate) {
        return IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(
                        day -> {
                            LocalDate date = month.atDay(day);
                            Overtime overtime = byDate.get(date);
                            return new CalendarDay(
                                    date, day, true, overtime, css(date, month, overtime));
                        })
                .toList();
    }

    private String css(LocalDate date, YearMonth month, Overtime overtime) {
        if (!YearMonth.from(date).equals(month)) return "muted-day";
        return overtime == null
                ? ""
                : "has-overtime status-" + overtime.getStatus().name().toLowerCase();
    }
}
