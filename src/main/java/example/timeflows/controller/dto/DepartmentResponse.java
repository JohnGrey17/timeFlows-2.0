package example.timeflows.controller.dto;

import example.timeflows.model.Department;
import java.util.List;

public record DepartmentResponse(
        Long id, String name, String description, List<DivisionSummaryResponse> divisions) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getDivisions().stream().map(DivisionSummaryResponse::from).toList());
    }
}
