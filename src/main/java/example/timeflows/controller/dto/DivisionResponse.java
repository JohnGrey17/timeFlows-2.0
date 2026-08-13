package example.timeflows.controller.dto;

import example.timeflows.model.Division;

import java.util.List;

public record DivisionResponse(
        Long id,
        String name,
        DepartmentSummaryResponse department,
        List<UserSummaryResponse> users
) {

    public static DivisionResponse from(Division division) {
        return new DivisionResponse(
                division.getId(),
                division.getName(),
                DepartmentSummaryResponse.from(division.getDepartment()),
                division.getUsers().stream()
                        .map(UserSummaryResponse::from)
                        .toList()
        );
    }
}
