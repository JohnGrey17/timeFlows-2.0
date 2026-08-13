package example.timeflows.controller.dto;

import example.timeflows.model.Overtime;

import java.time.LocalDate;

public record CalendarDay(
        LocalDate date,
        int dayOfMonth,
        boolean currentMonth,
        Overtime overtime,
        String cssClass
) {
}
