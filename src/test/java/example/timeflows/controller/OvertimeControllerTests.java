package example.timeflows.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.controller.dto.OvertimeDecisionRequest;
import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.service.OvertimeService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OvertimeControllerTests {

    @Mock private OvertimeService overtimeService;
    @Mock private Authentication authentication;

    private OvertimeController controller;

    @BeforeEach
    void setUp() {
        controller = new OvertimeController(overtimeService, new TimeflowsMapper());
        when(authentication.getName()).thenReturn("employee@vyriy.com");
    }

    @Test
    void employeeCrudEndpointsDelegateAndMapResponses() {
        Overtime overtime = overtime(1L, user(1L, 2L, Role.EMPLOYEE));
        OvertimeRequest request = request();
        when(overtimeService.findMonth("employee@vyriy.com", YearMonth.of(2026, 8)))
                .thenReturn(List.of(overtime));
        when(overtimeService.findByIdForUser(1L, "employee@vyriy.com")).thenReturn(overtime);
        when(overtimeService.create("employee@vyriy.com", request)).thenReturn(overtime);
        when(overtimeService.update("employee@vyriy.com", 1L, request)).thenReturn(overtime);
        when(overtimeService.resubmit("employee@vyriy.com", 1L, request)).thenReturn(overtime);

        assertThat(controller.findMonth(2026, 8, authentication)).hasSize(1);
        assertThat(controller.findById(1L, authentication).id()).isEqualTo(1L);
        assertThat(controller.create(request, authentication).status())
                .isEqualTo(OvertimeStatus.PENDING);
        assertThat(controller.update(1L, request, authentication).hours()).isEqualTo(2.0);
        assertThat(controller.resubmit(1L, request, authentication).user().id()).isEqualTo(1L);
        controller.delete(1L, authentication);

        verify(overtimeService).delete("employee@vyriy.com", 1L);
    }

    @Test
    void adminCanApproveWithoutDivisionCheck() {
        Overtime overtime = overtime(5L, user(2L, 2L, Role.EMPLOYEE));
        OvertimeDecisionRequest decision = new OvertimeDecisionRequest();
        decision.setManagerComment("Approved");
        when(overtimeService.approve(5L, "Approved", "employee@vyriy.com")).thenReturn(overtime);

        assertThat(controller.approve(5L, decision, authentication).id()).isEqualTo(5L);
        verify(overtimeService).approve(5L, "Approved", "employee@vyriy.com");
    }

    @Test
    void rejectDelegatesAuthorizationToService() {
        Overtime own = overtime(5L, user(2L, 2L, Role.EMPLOYEE));
        OvertimeDecisionRequest decision = new OvertimeDecisionRequest();
        decision.setManagerComment("Missing details");
        when(overtimeService.reject(5L, "Missing details", "employee@vyriy.com")).thenReturn(own);

        assertThat(controller.reject(5L, decision, authentication).id()).isEqualTo(5L);
        verify(overtimeService).reject(5L, "Missing details", "employee@vyriy.com");
    }

    private OvertimeRequest request() {
        OvertimeRequest request = new OvertimeRequest();
        request.setWorkDate(LocalDate.of(2026, 8, 14));
        request.setHours(2.0);
        request.setDescription("Work");
        request.setResubmissionReason("Updated");
        return request;
    }

    private Overtime overtime(Long id, User user) {
        Overtime overtime = new Overtime();
        overtime.setId(id);
        overtime.setUser(user);
        overtime.setWorkDate(LocalDate.of(2026, 8, 14));
        overtime.setHours(2.0);
        overtime.setDescription("Work");
        overtime.setStatus(OvertimeStatus.PENDING);
        return overtime;
    }

    private User user(Long id, Long divisionId, Role... roles) {
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        Division division = new Division();
        division.setId(divisionId);
        division.setName("Platform");
        division.setDepartment(department);
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@vyriy.com");
        user.setFirstName("User");
        user.setLastName(String.valueOf(id));
        user.setDivision(division);
        user.setRoles(new LinkedHashSet<>(Set.of(roles)));
        return user;
    }
}
