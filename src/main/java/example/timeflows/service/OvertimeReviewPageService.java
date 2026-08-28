package example.timeflows.service;

import example.timeflows.model.OvertimeStatus;
import java.util.Map;

public interface OvertimeReviewPageService {
    Map<String, Object> buildPage(
            String email,
            String mode,
            String view,
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            OvertimeStatus status,
            Integer year,
            Integer month,
            Long userId,
            Long openBonusUserId);
}
