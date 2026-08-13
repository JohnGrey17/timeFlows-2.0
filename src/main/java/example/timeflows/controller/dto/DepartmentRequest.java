package example.timeflows.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequest {

    @NotBlank(message = "Назва департаменту обов'язкова")
    @Size(max = 150, message = "Назва департаменту має містити до 150 символів")
    private String name;

    @Size(max = 500, message = "Опис має містити до 500 символів")
    private String description;
}
