package example.timeflows.service;

import java.util.Map;

public interface OvertimeReviewPageService {
    Map<String, Object> buildPage(
            String email,
            String view,
            Long departmentId,
            Long divisionId,
            Integer year,
            Integer month,
            Long openBonusUserId);
}
