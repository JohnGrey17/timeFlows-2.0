package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.exception.OvertimeException;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.OvertimeRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceImplTests {

    @Mock private OvertimeRepository overtimeRepository;

    @Mock private UserService userService;

    private OvertimeServiceImpl overtimeService;

    @BeforeEach
    void setUp() {
        overtimeService = new OvertimeServiceImpl(overtimeRepository, userService);
    }

    @Test
    void approveAllowsPendingOvertime() {
        Overtime overtime = overtime(OvertimeStatus.PENDING);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        Overtime result = overtimeService.approve(1L, "Погоджено");

        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.APPROVED);
        assertThat(result.getManagerComment()).isEqualTo("Погоджено");
        verify(overtimeRepository).save(overtime);
    }

    @Test
    void approveRejectsAlreadyDecidedOvertime() {
        Overtime overtime = overtime(OvertimeStatus.APPROVED);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));

        assertThatThrownBy(() -> overtimeService.approve(1L, null))
                .isInstanceOf(OvertimeException.class)
                .hasMessage("Рішення можна прийняти тільки для overtime, що очікує погодження");
        verify(overtimeRepository, never()).save(overtime);
    }

    @Test
    void rejectRejectsAlreadyDecidedOvertime() {
        Overtime overtime = overtime(OvertimeStatus.REJECTED);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));

        assertThatThrownBy(() -> overtimeService.reject(1L, "Повторне рішення"))
                .isInstanceOf(OvertimeException.class)
                .hasMessage("Рішення можна прийняти тільки для overtime, що очікує погодження");
        verify(overtimeRepository, never()).save(overtime);
    }

    @Test
    void rejectRequiresReason() {
        assertThatThrownBy(() -> overtimeService.reject(1L, "  "))
                .isInstanceOf(example.timeflows.exception.OvertimeException.class)
                .hasMessageContaining("Причина відхилення");
        verifyNoInteractions(overtimeRepository);
    }

    @Test
    void resubmitRequiresReasonAtServiceBoundary() {
        Overtime overtime = overtime(OvertimeStatus.REJECTED);
        when(overtimeRepository.findByIdAndUserEmail(1L, "employee@vyriy.com"))
                .thenReturn(Optional.of(overtime));
        OvertimeRequest request = validRequest();
        request.setResubmissionReason("   ");

        assertThatThrownBy(() -> overtimeService.resubmit("employee@vyriy.com", 1L, request))
                .isInstanceOf(OvertimeException.class)
                .hasMessage("Причина повторного погодження обов'язкова");
        verify(overtimeRepository, never()).save(overtime);
    }

    @Test
    void monthQueriesUseMonthBoundaries() {
        YearMonth month = YearMonth.of(2026, 8);
        Overtime overtime = overtime(OvertimeStatus.PENDING);
        when(overtimeRepository.findByUserEmailAndWorkDateBetweenOrderByWorkDateAsc(
                        "employee@vyriy.com", month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(overtime));
        when(overtimeRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(overtime));
        when(overtimeRepository
                        .findByUserDivisionIdAndWorkDateBetweenOrderByUserEmailAscWorkDateAsc(
                                2L, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(overtime));
        when(overtimeRepository
                        .findByUserDivisionDepartmentIdAndWorkDateBetweenOrderByUserEmailAscWorkDateAsc(
                                3L, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(overtime));

        assertThat(overtimeService.findMonth("employee@vyriy.com", month))
                .containsExactly(overtime);
        assertThat(overtimeService.findUserMonth(1L, month)).containsExactly(overtime);
        assertThat(overtimeService.findDivisionMonth(2L, month)).containsExactly(overtime);
        assertThat(overtimeService.findDepartmentMonth(3L, month)).containsExactly(overtime);
    }

    @Test
    void createPendingOvertimeForEmployee() {
        OvertimeRequest request = validRequest();
        User employee = user(Role.EMPLOYEE);
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(employee);
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Overtime result = overtimeService.create("employee@vyriy.com", request);

        assertThat(result.getUser()).isSameAs(employee);
        assertThat(result.getWorkDate()).isEqualTo(request.getWorkDate());
        assertThat(result.getHours()).isEqualTo(request.getHours());
        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.PENDING);
    }

    @Test
    void createAutoApprovesAdminAndRejectsDuplicateOrExcessHours() {
        OvertimeRequest request = validRequest();
        User admin = user(Role.ADMIN);
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(overtimeService.create("admin@vyriy.com", request).getStatus())
                .isEqualTo(OvertimeStatus.APPROVED);

        when(overtimeRepository.existsByUserEmailAndWorkDate(
                        "employee@vyriy.com", request.getWorkDate()))
                .thenReturn(true);
        assertThatThrownBy(() -> overtimeService.create("employee@vyriy.com", request))
                .isInstanceOf(OvertimeException.class);

        OvertimeRequest excessive = validRequest();
        excessive.setHours(7.0);
        assertThatThrownBy(() -> overtimeService.create("another@vyriy.com", excessive))
                .isInstanceOf(OvertimeException.class);
    }

    @Test
    void weekendAllowsUpToFourteenHours() {
        OvertimeRequest request = validRequest();
        request.setWorkDate(LocalDate.of(2026, 8, 15));
        request.setHours(14.0);
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(user(Role.EMPLOYEE));
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(overtimeService.create("employee@vyriy.com", request).getHours())
                .isEqualTo(14.0);
    }

    @Test
    void updateRejectsAnotherEntryOnSameDate() {
        Overtime overtime = overtime(OvertimeStatus.PENDING);
        Overtime duplicate = overtime(OvertimeStatus.PENDING);
        duplicate.setId(2L);
        OvertimeRequest request = validRequest();
        when(overtimeRepository.findByIdAndUserEmail(1L, "employee@vyriy.com"))
                .thenReturn(Optional.of(overtime));
        when(overtimeRepository.findByUserEmailAndWorkDate(
                        "employee@vyriy.com", request.getWorkDate()))
                .thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> overtimeService.update("employee@vyriy.com", 1L, request))
                .isInstanceOf(OvertimeException.class);
    }

    @Test
    void deleteAllowsPendingAndRejectsFinalForOrdinaryEmployee() {
        Overtime pending = overtime(OvertimeStatus.PENDING);
        when(overtimeRepository.findByIdAndUserEmail(1L, "employee@vyriy.com"))
                .thenReturn(Optional.of(pending));
        overtimeService.delete("employee@vyriy.com", 1L);
        verify(overtimeRepository).delete(pending);

        Overtime approved = overtime(OvertimeStatus.APPROVED);
        approved.setWorkDate(LocalDate.now());
        when(overtimeRepository.findByIdAndUserEmail(2L, "employee@vyriy.com"))
                .thenReturn(Optional.of(approved));
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(user(Role.EMPLOYEE));
        assertThatThrownBy(() -> overtimeService.delete("employee@vyriy.com", 2L))
                .isInstanceOf(OvertimeException.class);
    }

    @Test
    void resubmitRejectedOvertimeReturnsItToPending() {
        Overtime overtime = overtime(OvertimeStatus.REJECTED);
        when(overtimeRepository.findByIdAndUserEmail(1L, "employee@vyriy.com"))
                .thenReturn(Optional.of(overtime));
        when(overtimeRepository.save(overtime)).thenReturn(overtime);
        OvertimeRequest request = validRequest();
        request.setResubmissionReason("Corrected details");

        Overtime result = overtimeService.resubmit("employee@vyriy.com", 1L, request);

        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.PENDING);
        assertThat(result.getResubmissionReason()).isEqualTo("Corrected details");
        assertThat(result.getManagerComment()).isNull();
    }

    @Test
    void rejectPendingOvertimeStoresComment() {
        Overtime overtime = overtime(OvertimeStatus.PENDING);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        Overtime result = overtimeService.reject(1L, "Missing details");

        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.REJECTED);
        assertThat(result.getManagerComment()).isEqualTo("Missing details");
    }

    private Overtime overtime(OvertimeStatus status) {
        Overtime overtime = new Overtime();
        overtime.setId(1L);
        overtime.setStatus(status);
        return overtime;
    }

    private OvertimeRequest validRequest() {
        OvertimeRequest request = new OvertimeRequest();
        request.setWorkDate(LocalDate.of(2026, 8, 13));
        request.setHours(2.0);
        request.setDescription("Опис роботи");
        return request;
    }

    private User user(Role... roles) {
        User user = new User();
        user.setRoles(new LinkedHashSet<>(Set.of(roles)));
        return user;
    }
}
