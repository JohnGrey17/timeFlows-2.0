package example.timeflows.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.controller.dto.DeactivateUserRequest;
import example.timeflows.controller.dto.DepartmentRequest;
import example.timeflows.controller.dto.DivisionRequest;
import example.timeflows.controller.dto.UserRequest;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.UserService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrudControllerTests {

    private final TimeflowsMapper mapper = new TimeflowsMapper();

    @Mock private DepartmentService departmentService;
    @Mock private DivisionService divisionService;
    @Mock private UserService userService;

    @Test
    void departmentControllerMapsEveryCrudOperation() {
        DepartmentController controller = new DepartmentController(departmentService, mapper);
        Department department = department();
        when(departmentService.findAll()).thenReturn(List.of(department));
        when(departmentService.findById(1L)).thenReturn(department);
        when(departmentService.create(any(Department.class))).thenReturn(department);
        when(departmentService.update(org.mockito.ArgumentMatchers.eq(1L), any(Department.class)))
                .thenReturn(department);
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Engineering");
        request.setDescription("Product development");

        assertThat(controller.findAll()).hasSize(1);
        assertThat(controller.findById(1L).name()).isEqualTo("Engineering");
        assertThat(controller.create(request).id()).isEqualTo(1L);
        assertThat(controller.update(1L, request).description()).isEqualTo("Product development");
        controller.delete(1L);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentService).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Engineering");
        verify(departmentService).delete(1L);
    }

    @Test
    void divisionControllerMapsEveryCrudOperation() {
        DivisionController controller = new DivisionController(divisionService, mapper);
        Division division = division();
        when(divisionService.findAll()).thenReturn(List.of(division));
        when(divisionService.findById(2L)).thenReturn(division);
        when(divisionService.create(any(Division.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(division);
        when(divisionService.update(
                        org.mockito.ArgumentMatchers.eq(2L),
                        any(Division.class),
                        org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(division);
        DivisionRequest request = new DivisionRequest();
        request.setName("Platform");
        request.setDepartmentId(1L);

        assertThat(controller.findAll()).hasSize(1);
        assertThat(controller.findById(2L).name()).isEqualTo("Platform");
        assertThat(controller.create(request).department().id()).isEqualTo(1L);
        assertThat(controller.update(2L, request).id()).isEqualTo(2L);
        controller.delete(2L);

        verify(divisionService).delete(2L);
    }

    @Test
    void userControllerMapsEveryCrudOperation() {
        UserController controller = new UserController(userService, mapper);
        User user = user();
        when(userService.findAll()).thenReturn(List.of(user));
        when(userService.findById(3L)).thenReturn(user);
        when(userService.create(any(User.class), org.mockito.ArgumentMatchers.eq(2L)))
                .thenReturn(user);
        when(userService.update(
                        org.mockito.ArgumentMatchers.eq(3L),
                        any(User.class),
                        org.mockito.ArgumentMatchers.eq(2L)))
                .thenReturn(user);
        UserRequest request = new UserRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setEmail("ada@vyriy.com");
        request.setPassword("password");
        request.setRoles(new LinkedHashSet<>(Set.of(Role.EMPLOYEE)));
        request.setDivisionId(2L);

        assertThat(controller.findAll()).hasSize(1);
        assertThat(controller.findById(3L).email()).isEqualTo("ada@vyriy.com");
        assertThat(controller.create(request).firstName()).isEqualTo("Ada");
        assertThat(controller.update(3L, request).id()).isEqualTo(3L);
        DeactivateUserRequest deactivate = new DeactivateUserRequest();
        deactivate.setReason("Left company");
        controller.delete(3L, deactivate);

        verify(userService).deactivate(3L, "Left company");
    }

    @Test
    void dashboardRoutesToApplicationPages() {
        DashboardController controller = new DashboardController();
        assertThat(controller.index()).isEqualTo("redirect:/api/dashboard");
        assertThat(controller.dashboard()).isEqualTo("redirect:/api/overtime");
    }

    private Department department() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        department.setDescription("Product development");
        return department;
    }

    private Division division() {
        Division division = new Division();
        division.setId(2L);
        division.setName("Platform");
        division.setDepartment(department());
        return division;
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@vyriy.com");
        user.setRoles(new LinkedHashSet<>(Set.of(Role.EMPLOYEE)));
        user.setDivision(division());
        user.setActive(true);
        return user;
    }
}
