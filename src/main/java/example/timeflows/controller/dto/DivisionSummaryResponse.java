package example.timeflows.controller.dto;

import example.timeflows.model.Division;

public record DivisionSummaryResponse(Long id, String name, Long departmentId, String departmentName) {

    public static DivisionSummaryResponse from(Division division) {
        return new DivisionSummaryResponse(
                division.getId(),
                division.getName(),
                division.getDepartment().getId(),
                division.getDepartment().getName()
        );
    }
}
