package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.exception.OvertimeException;
import example.timeflows.model.BusinessTag;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.OvertimeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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
        overtimeService = serviceAt("2026-08-10T08:00:00Z");
    }

    @Test
    void approveAllowsPendingOvertime() {
        Overtime overtime = overtime(OvertimeStatus.PENDING);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        Overtime result = overtimeService.approve(1L, "Погоджено");

        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.APPROVED_MANAGER);
        assertThat(result.getManagerComment()).isEqualTo("Погоджено");
        verify(overtimeRepository).save(overtime);
    }

    @Test
    void approveRejectsAlreadyDecidedOvertime() {
        Overtime overtime = overtime(OvertimeStatus.APPROVED_ADMIN);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));

        assertThatThrownBy(() -> overtimeService.approve(1L, null))
                .isInstanceOf(OvertimeException.class)
                .hasMessageContaining("CHECKING");
        verify(overtimeRepository, never()).save(overtime);
    }

    @Test
    void rejectRejectsAlreadyDecidedOvertime() {
        Overtime overtime = overtime(OvertimeStatus.DECLINED);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));

        assertThatThrownBy(() -> overtimeService.reject(1L, "Повторне рішення"))
                .isInstanceOf(OvertimeException.class)
                .hasMessage("Відхилити можна лише заявку на погодженні");
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
        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.CHECKING);
    }

    @Test
    void createAutoApprovesAdminAndRejectsDuplicateOrExcessHours() {
        OvertimeRequest request = validRequest();
        User admin = user(Role.ADMIN);
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(overtimeService.create("admin@vyriy.com", request).getStatus())
                .isEqualTo(OvertimeStatus.APPROVED_ADMIN);

        when(overtimeRepository.existsByUserEmailAndWorkDate(
                        "employee@vyriy.com", request.getWorkDate()))
                .thenReturn(true);
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(user(Role.EMPLOYEE));
        assertThatThrownBy(() -> overtimeService.create("employee@vyriy.com", request))
                .isInstanceOf(OvertimeException.class);

        OvertimeRequest excessive = validRequest();
        excessive.setHours(15.0);
        when(userService.findByEmail("another@vyriy.com")).thenReturn(user(Role.EMPLOYEE));
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
    void august2026AllowsAnyDayForEveryUser() {
        User employee = user(Role.EMPLOYEE);
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(employee);
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        OvertimeRequest weekday = validRequest();
        weekday.setWorkDate(LocalDate.of(2026, 8, 12));

        Overtime created = overtimeService.create("employee@vyriy.com", weekday);

        assertThat(created.getWorkDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(created.getStatus()).isEqualTo(OvertimeStatus.CHECKING);

        OvertimeRequest pastWeekday = validRequest();
        pastWeekday.setWorkDate(LocalDate.of(2026, 8, 3));
        assertThat(overtimeService.create("employee@vyriy.com", pastWeekday).getWorkDate())
                .isEqualTo(LocalDate.of(2026, 8, 3));
    }

    @Test
    void ordinaryUserCanSubmitPastWeekendInCurrentMonthAndFutureWeekend() {
        overtimeService = serviceAt("2026-09-15T08:00:00Z");
        User employee = user(Role.EMPLOYEE);
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(employee);
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OvertimeRequest pastCurrentMonthWeekend = validRequest();
        pastCurrentMonthWeekend.setWorkDate(LocalDate.of(2026, 9, 5));
        assertThat(
                        overtimeService
                                .create("employee@vyriy.com", pastCurrentMonthWeekend)
                                .getWorkDate())
                .isEqualTo(LocalDate.of(2026, 9, 5));

        OvertimeRequest futureWeekend = validRequest();
        futureWeekend.setWorkDate(LocalDate.of(2026, 10, 3));
        assertThat(overtimeService.create("employee@vyriy.com", futureWeekend).getWorkDate())
                .isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void ordinaryUserCannotSubmitPastWeekendFromPreviousMonthOrWeekday() {
        overtimeService = serviceAt("2026-09-15T08:00:00Z");
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(user(Role.EMPLOYEE));

        OvertimeRequest previousMonthWeekend = validRequest();
        previousMonthWeekend.setWorkDate(LocalDate.of(2026, 7, 26));
        assertThatThrownBy(() -> overtimeService.create("employee@vyriy.com", previousMonthWeekend))
                .isInstanceOf(OvertimeException.class)
                .hasMessageContaining("вихідні поточного місяця");

        OvertimeRequest weekday = validRequest();
        weekday.setWorkDate(LocalDate.of(2026, 9, 16));
        assertThatThrownBy(() -> overtimeService.create("employee@vyriy.com", weekday))
                .isInstanceOf(OvertimeException.class)
                .hasMessageContaining("вихідні поточного місяця");
    }

    @Test
    void allowOverTagStillAllowsAnyDayOnlyWithinCurrentWeekOutsideAugust() {
        overtimeService = serviceAt("2026-09-14T08:00:00Z");
        User employee = user(Role.EMPLOYEE);
        employee.getTags().add(BusinessTag.ALLOW_OVER);
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(employee);
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OvertimeRequest weekday = validRequest();
        weekday.setWorkDate(LocalDate.of(2026, 9, 16));
        assertThat(overtimeService.create("employee@vyriy.com", weekday).getWorkDate())
                .isEqualTo(LocalDate.of(2026, 9, 16));

        OvertimeRequest nextWeek = validRequest();
        nextWeek.setWorkDate(LocalDate.of(2026, 9, 21));
        assertThatThrownBy(() -> overtimeService.create("employee@vyriy.com", nextWeek))
                .isInstanceOf(OvertimeException.class)
                .hasMessageContaining("поточного тижня");
    }

    private OvertimeServiceImpl serviceAt(String instant) {
        return new OvertimeServiceImpl(
                overtimeRepository,
                userService,
                Clock.fixed(Instant.parse(instant), ZoneId.of("Europe/Kyiv")));
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

        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.CHECKING);
        assertThat(result.getResubmissionReason()).isEqualTo("Corrected details");
        assertThat(result.getManagerComment()).isNull();
    }

    @Test
    void rejectPendingOvertimeStoresComment() {
        Overtime overtime = overtime(OvertimeStatus.PENDING);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(overtime));
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        Overtime result = overtimeService.reject(1L, "Missing details");

        assertThat(result.getStatus()).isEqualTo(OvertimeStatus.DECLINED);
        assertThat(result.getManagerComment()).isEqualTo("Missing details");
    }

    @Test
    void managerAndAdminApproveSeparateWorkflowStages() {
        Division division = new Division();
        division.setId(5L);
        User manager = user(Role.EMPLOYEE, Role.MANAGER);
        manager.setId(10L);
        manager.setDivision(division);
        division.setManager(manager);
        User employee = user(Role.EMPLOYEE);
        employee.setDivision(division);
        Overtime checking = overtime(OvertimeStatus.CHECKING);
        checking.setWorkDate(LocalDate.of(2026, 8, 12));
        checking.setUser(employee);
        when(userService.findByEmail("manager@vyriy.com")).thenReturn(manager);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(checking));
        when(overtimeRepository.save(checking)).thenReturn(checking);

        assertThat(overtimeService.approve(1L, null, "manager@vyriy.com").getStatus())
                .isEqualTo(OvertimeStatus.APPROVED_MANAGER);

        User admin = user(Role.ADMIN);
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        assertThat(overtimeService.approve(1L, null, "admin@vyriy.com").getStatus())
                .isEqualTo(OvertimeStatus.APPROVED_ADMIN);
    }

    @Test
    void adminBulkApprovalApprovesAllManagerApprovedOvertimesWithoutDeadline() {
        User admin = user(Role.ADMIN);
        Overtime eligible = overtime(OvertimeStatus.APPROVED_MANAGER);
        eligible.setId(1L);
        Overtime alreadyApproved = overtime(OvertimeStatus.APPROVED_ADMIN);
        alreadyApproved.setId(2L);
        Overtime olderApproved = overtime(OvertimeStatus.APPROVED_MANAGER);
        olderApproved.setId(3L);
        olderApproved.setWorkDate(LocalDate.of(2026, 8, 9));
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        when(overtimeRepository.findWithUserById(1L)).thenReturn(Optional.of(eligible));
        when(overtimeRepository.findWithUserById(2L)).thenReturn(Optional.of(alreadyApproved));
        when(overtimeRepository.findWithUserById(3L)).thenReturn(Optional.of(olderApproved));

        int approved =
                overtimeService.approveAll(
                        List.of(1L, 2L, 3L), "Погоджено масово", "admin@vyriy.com");

        assertThat(approved).isEqualTo(2);
        assertThat(eligible.getStatus()).isEqualTo(OvertimeStatus.APPROVED_ADMIN);
        assertThat(eligible.getManagerComment()).isEqualTo("Погоджено масово");
        verify(overtimeRepository).save(eligible);
        verify(overtimeRepository, never()).save(alreadyApproved);
        assertThat(olderApproved.getStatus()).isEqualTo(OvertimeStatus.APPROVED_ADMIN);
        verify(overtimeRepository).save(olderApproved);
    }

    @Test
    void submissionAfterFridayElevenIsAllowedForWeekend() {
        OvertimeServiceImpl lateService = serviceAt("2026-09-18T08:01:00Z");
        when(userService.findByEmail("employee@vyriy.com")).thenReturn(user(Role.EMPLOYEE));
        when(overtimeRepository.save(org.mockito.ArgumentMatchers.any(Overtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        OvertimeRequest request = validRequest();
        request.setWorkDate(LocalDate.of(2026, 9, 19));

        assertThat(lateService.create("employee@vyriy.com", request).getWorkDate())
                .isEqualTo(LocalDate.of(2026, 9, 19));
    }

    private Overtime overtime(OvertimeStatus status) {
        Overtime overtime = new Overtime();
        overtime.setId(1L);
        overtime.setStatus(status);
        overtime.setWorkDate(LocalDate.of(2026, 8, 15));
        overtime.setUser(user(Role.EMPLOYEE));
        return overtime;
    }

    private OvertimeRequest validRequest() {
        OvertimeRequest request = new OvertimeRequest();
        request.setWorkDate(LocalDate.of(2026, 8, 15));
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
