package example.timeflows.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OvertimePaymentDetail(
        LocalDate date, double hours, String description, BigDecimal amount) {}
