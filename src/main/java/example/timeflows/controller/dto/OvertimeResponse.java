package example.timeflows.controller.dto;

import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import java.time.LocalDate;

public record OvertimeResponse(
        Long id,
        LocalDate workDate,
        Double hours,
        String description,
        OvertimeStatus status,
        String managerComment,
        String resubmissionReason,
        UserSummaryResponse user) {

    public static OvertimeResponse from(Overtime overtime) {
        return new OvertimeResponse(
                overtime.getId(),
                overtime.getWorkDate(),
                overtime.getHours(),
                overtime.getDescription(),
                overtime.getStatus(),
                overtime.getManagerComment(),
                overtime.getResubmissionReason(),
                UserSummaryResponse.from(overtime.getUser()));
    }
}
