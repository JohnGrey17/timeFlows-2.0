package example.timeflows.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DivisionRequest {

    @NotBlank(message = "Назва відділу обов'язкова")
    @Size(max = 150, message = "Назва відділу має містити до 150 символів")
    private String name;

    @NotNull(message = "Оберіть департамент")
    private Long departmentId;
}
