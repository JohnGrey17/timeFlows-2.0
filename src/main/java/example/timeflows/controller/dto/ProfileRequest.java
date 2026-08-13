package example.timeflows.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequest {

    @Size(max = 100, message = "Ім'я має містити до 100 символів")
    private String firstName;

    @Size(max = 100, message = "Прізвище має містити до 100 символів")
    private String lastName;
}
