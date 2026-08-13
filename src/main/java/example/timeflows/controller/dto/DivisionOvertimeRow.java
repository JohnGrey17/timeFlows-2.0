package example.timeflows.controller.dto;

import example.timeflows.model.User;

import java.util.List;

public record DivisionOvertimeRow(
        User user,
        List<CalendarDay> days
) {
}
