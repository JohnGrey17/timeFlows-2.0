package example.timeflows.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OvertimeRequest {

    @NotNull(message = "Дата обов'язкова")
    private LocalDate workDate;

    @NotNull(message = "Кількість годин обов'язкова")
    @Positive(message = "Кількість годин має бути більшою за 0")
    private Double hours;

    @NotBlank(message = "Опис обов'язковий")
    @Size(max = 1000, message = "Опис має містити до 1000 символів")
    private String description;

    @Size(max = 1000, message = "Причина повторного погодження має містити до 1000 символів")
    @NotBlank(groups = Resubmission.class, message = "Причина повторного погодження обов'язкова")
    private String resubmissionReason;

    public interface Resubmission {
    }
}
