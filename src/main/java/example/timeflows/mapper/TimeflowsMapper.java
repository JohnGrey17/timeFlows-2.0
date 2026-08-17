package example.timeflows.mapper;

import example.timeflows.controller.dto.DepartmentRequest;
import example.timeflows.controller.dto.DepartmentResponse;
import example.timeflows.controller.dto.DepartmentSummaryResponse;
import example.timeflows.controller.dto.DivisionRequest;
import example.timeflows.controller.dto.DivisionResponse;
import example.timeflows.controller.dto.DivisionSummaryResponse;
import example.timeflows.controller.dto.OvertimeResponse;
import example.timeflows.controller.dto.ProfileRequest;
import example.timeflows.controller.dto.UserRequest;
import example.timeflows.controller.dto.UserResponse;
import example.timeflows.controller.dto.UserSummaryResponse;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TimeflowsMapper {

    public Department toDepartment(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        return department;
    }

    public DepartmentResponse toDepartmentResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getDivisions().stream().map(this::toDivisionSummary).toList());
    }

    public DepartmentSummaryResponse toDepartmentSummary(Department department) {
        return new DepartmentSummaryResponse(department.getId(), department.getName());
    }

    public Division toDivision(DivisionRequest request) {
        Division division = new Division();
        division.setName(request.getName());
        return division;
    }

    public DivisionResponse toDivisionResponse(Division division) {
        return new DivisionResponse(
                division.getId(),
                division.getName(),
                toDepartmentSummary(division.getDepartment()),
                division.getUsers().stream().map(this::toUserSummary).toList());
    }

    public DivisionSummaryResponse toDivisionSummary(Division division) {
        return new DivisionSummaryResponse(
                division.getId(),
                division.getName(),
                division.getDepartment().getId(),
                division.getDepartment().getName());
    }

    public User toUser(UserRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRoles(request.getRoles());
        return user;
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles(),
                user.getDivision() == null ? null : toDivisionSummary(user.getDivision()),
                user.getSalary(),
                user.isActive(),
                user.getDeactivationReason(),
                user.getCreatedAt());
    }

    public UserSummaryResponse toUserSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles());
    }

    public OvertimeResponse toOvertimeResponse(Overtime overtime) {
        return new OvertimeResponse(
                overtime.getId(),
                overtime.getWorkDate(),
                overtime.getHours(),
                overtime.getDescription(),
                overtime.getStatus(),
                overtime.getManagerComment(),
                overtime.getResubmissionReason(),
                toUserSummary(overtime.getUser()));
    }

    public List<DepartmentResponse> toDepartmentResponses(List<Department> departments) {
        return departments.stream().map(this::toDepartmentResponse).toList();
    }

    public List<DivisionSummaryResponse> toDivisionSummaries(List<Division> divisions) {
        return divisions.stream().map(this::toDivisionSummary).toList();
    }

    public List<UserResponse> toUserResponses(List<User> users) {
        return users.stream().map(this::toUserResponse).toList();
    }

    public List<OvertimeResponse> toOvertimeResponses(List<Overtime> overtimes) {
        return overtimes.stream().map(this::toOvertimeResponse).toList();
    }

    public ProfileRequest toProfileRequest(User user) {
        ProfileRequest request = new ProfileRequest();
        request.setFirstName(user.getFirstName());
        request.setLastName(user.getLastName());
        return request;
    }
}
