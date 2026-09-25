package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.OvertimeStatus;
import org.junit.jupiter.api.Test;

class OvertimeReviewPageServiceImplTests {

    @Test
    void directorateManagerKeepsBulkApprovedRequestsVisible() {
        assertThat(
                        OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                OvertimeStatus.APPROVED_DIRECTORATE, false, true, false))
                .isTrue();
    }

    @Test
    void directorateManagerDoesNotSeeRequestsBeforeDivisionApproval() {
        assertThat(
                        OvertimeReviewPageServiceImpl.isVisibleForReviewer(
                                OvertimeStatus.CHECKING, false, true, false))
                .isFalse();
    }
}
