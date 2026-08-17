package example.timeflows.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OvertimeRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void regularOvertimeDoesNotRequireResubmissionReason() {
        Set<ConstraintViolation<OvertimeRequest>> violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void resubmissionRequiresNonBlankReason() {
        OvertimeRequest request = validRequest();
        request.setResubmissionReason("   ");

        Set<ConstraintViolation<OvertimeRequest>> violations =
                validator.validate(request, Default.class, OvertimeRequest.Resubmission.class);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Причина повторного погодження обов'язкова");
    }

    private OvertimeRequest validRequest() {
        OvertimeRequest request = new OvertimeRequest();
        request.setWorkDate(LocalDate.of(2026, 8, 13));
        request.setHours(2.0);
        request.setDescription("Опис роботи");
        return request;
    }
}
