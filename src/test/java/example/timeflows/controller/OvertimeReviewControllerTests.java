package example.timeflows.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import example.timeflows.model.OvertimeStatus;
import example.timeflows.service.OvertimeReviewExcelService;
import example.timeflows.service.OvertimeReviewPageService;
import example.timeflows.service.OvertimeService;
import example.timeflows.service.SavedOvertimeFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class OvertimeReviewControllerTests {

    private OvertimeService overtimeService;
    private OvertimeReviewController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        overtimeService = mock(OvertimeService.class);
        controller =
                new OvertimeReviewController(
                        overtimeService,
                        mock(OvertimeReviewPageService.class),
                        mock(OvertimeReviewExcelService.class),
                        mock(SavedOvertimeFilterService.class));
        authentication = mock(Authentication.class);
        org.mockito.Mockito.when(authentication.getName()).thenReturn("manager@vyriy.com");
    }

    @Test
    void approveKeepsSelectedMonthAndFilters() {
        String redirect =
                controller.approve(
                        15L,
                        "Погоджено",
                        "division",
                        "matrix",
                        1L,
                        2L,
                        3L,
                        4L,
                        OvertimeStatus.CHECKING,
                        null,
                        2026,
                        9,
                        authentication);

        verify(overtimeService).approve(15L, "Погоджено", "manager@vyriy.com");
        assertThat(redirect)
                .contains("year=2026", "month=9", "divisionId=3", "status=CHECKING")
                .doesNotContain("month=8");
    }

    @Test
    void rejectKeepsEmployeeModeAndSelectedUser() {
        String redirect =
                controller.reject(
                        16L,
                        "Недостатньо даних",
                        "employee",
                        "matrix",
                        null,
                        null,
                        3L,
                        null,
                        null,
                        25L,
                        2026,
                        9,
                        authentication);

        verify(overtimeService).reject(16L, "Недостатньо даних", "manager@vyriy.com");
        assertThat(redirect).contains("mode=employee", "userId=25", "year=2026", "month=9");
    }
}
