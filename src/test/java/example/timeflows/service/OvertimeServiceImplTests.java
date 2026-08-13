package example.timeflows.service;

import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.exception.OvertimeException;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.repository.OvertimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceImplTests {

    @Mock
    private OvertimeRepository overtimeRepository;

    @Mock
    private UserService userService;

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
}
