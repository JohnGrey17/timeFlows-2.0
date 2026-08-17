package example.timeflows.controller.dto;

import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Set<Role> roles,
        DivisionSummaryResponse division,
        BigDecimal salary,
        boolean active,
        String deactivationReason,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles(),
                DivisionSummaryResponse.from(user.getDivision()),
                user.getSalary(),
                user.isActive(),
                user.getDeactivationReason(),
                user.getCreatedAt());
    }
}
