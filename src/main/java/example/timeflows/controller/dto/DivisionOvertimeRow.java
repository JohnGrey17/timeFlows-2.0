package example.timeflows.controller.dto;

import example.timeflows.model.User;

import java.util.List;
import java.math.BigDecimal;

public record DivisionOvertimeRow(
        User user,
        List<CalendarDay> days,
        BigDecimal bonusTotal,
        BigDecimal baseSalary,
        BigDecimal overtimeHours,
        BigDecimal overtimeAmount,
        BigDecimal totalPayment,
        List<OvertimePaymentDetail> overtimeDetails
) {
}
