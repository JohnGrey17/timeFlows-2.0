package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.OvertimeStatus;
import org.junit.jupiter.api.Test;

class OvertimeReviewPageServiceImplTests {

    @Test
    void directorateManagerKeepsBulkApprovedRequestsVisible() {
        assertThat(
                        OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                OvertimeStatus.APPROVED_DIRECTORATE, false, true, false, false))
                .isTrue();
    }

    @Test
    void adminSeesDirectorateApprovedRequests() {
        assertThat(
                        OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                OvertimeStatus.APPROVED_DIRECTORATE, true, false, false, false))
                .isTrue();
    }

    @Test
    void adminSeesRequestsAtEveryStatus() {
        assertThat(OvertimeStatus.values())
                .allSatisfy(
                        status ->
                                assertThat(
                                                OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                                        status, true, false, false, false))
                                        .isTrue());
    }

    @Test
    void directorateManagerDoesNotSeeRequestsBeforeDivisionApproval() {
        assertThat(
                        OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                OvertimeStatus.CHECKING, false, true, false, false))
                .isFalse();
    }

    @Test
    void fullManagementDirectorateManagerSeesRequestsAtEveryStatus() {
        assertThat(OvertimeStatus.values())
                .allSatisfy(
                        status ->
                                assertThat(
                                                OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                                        status, false, true, true, false))
                                        .isTrue());
    }
}
