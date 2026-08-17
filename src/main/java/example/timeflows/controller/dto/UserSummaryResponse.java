package example.timeflows.controller.dto;

import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.util.Set;

public record UserSummaryResponse(
        Long id, String firstName, String lastName, String email, Set<Role> roles) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles());
    }
}
