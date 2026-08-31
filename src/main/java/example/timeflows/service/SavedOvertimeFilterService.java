package example.timeflows.service;

import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.SavedOvertimeFilter;
import java.util.List;

public interface SavedOvertimeFilterService {

    List<SavedOvertimeFilter> findForAdmin(String email);

    SavedOvertimeFilter save(
            String email,
            String name,
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            OvertimeStatus status,
            Integer year,
            Integer month);
}
