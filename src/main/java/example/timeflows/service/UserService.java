package example.timeflows.service;

import example.timeflows.controller.dto.RegisterRequest;
import example.timeflows.model.BusinessTag;
import example.timeflows.model.Division;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface UserService {

    List<User> findAll();

    User findById(Long id);

    User register(RegisterRequest request);

    User findByEmail(String email);

    List<User> findActiveUsers();

    List<User> findDeactivatedUsers();

    List<User> findActiveUsersByDivision(Long divisionId);

    List<User> findActiveUsersByDepartment(Long departmentId);

    List<User> findActiveUsersByDirectorate(Long directorateId);

    List<User> findActiveUsersBySubdivision(Long subdivisionId);

    User create(User user, Long divisionId);

    User update(Long id, User user, Long divisionId);

    void delete(Long id);

    void deactivate(Long id, String reason);

    User updateSalary(Long id, BigDecimal salary);

    User assignDivisionManager(Long divisionId, Long userId);

    User moveToOrganization(Long userId, Long divisionId, Long subdivisionId);

    User updateRoles(Long userId, Set<Role> roles, String actorEmail);

    User updateTags(Long userId, Set<BusinessTag> tags);

    Division updateDivisionTags(Long divisionId, Set<BusinessTag> tags);

    void updateProfile(String email, String firstName, String lastName);

    void changePassword(
            String email, String currentPassword, String newPassword, String confirmPassword);
}
