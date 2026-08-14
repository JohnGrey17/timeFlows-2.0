package example.timeflows.service;

import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.model.Overtime;

import java.time.YearMonth;
import java.util.List;

public interface OvertimeService {

    List<Overtime> findMonth(String userEmail, YearMonth month);

    List<Overtime> findUserMonth(Long userId, YearMonth month);

    List<Overtime> findDivisionMonth(Long divisionId, YearMonth month);

    List<Overtime> findDepartmentMonth(Long departmentId, YearMonth month);

    Overtime findByIdForUser(Long id, String userEmail);

    Overtime findById(Long id);

    Overtime create(String userEmail, OvertimeRequest request);

    Overtime update(String userEmail, Long id, OvertimeRequest request);

    void delete(String userEmail, Long id);

    Overtime resubmit(String userEmail, Long id, OvertimeRequest request);

    Overtime approve(Long id, String managerComment);

    Overtime reject(Long id, String managerComment);
}
