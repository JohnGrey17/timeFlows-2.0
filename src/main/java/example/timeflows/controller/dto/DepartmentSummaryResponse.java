package example.timeflows.controller.dto;

import example.timeflows.model.Department;

public record DepartmentSummaryResponse(Long id, String name) {

    public static DepartmentSummaryResponse from(Department department) {
        return new DepartmentSummaryResponse(department.getId(), department.getName());
    }
}
