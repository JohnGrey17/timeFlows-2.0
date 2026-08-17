package example.timeflows.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Вкажіть ім'я")
    @Size(max = 100, message = "Ім'я має містити до 100 символів")
    private String firstName;

    @NotBlank(message = "Вкажіть прізвище")
    @Size(max = 100, message = "Прізвище має містити до 100 символів")
    private String lastName;

    @NotBlank(message = "Вкажіть email")
    @Email(message = "Вкажіть коректний email")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@vyriy\\.com$",
            message = "Будь ласка вкажіть корпоративний email")
    private String email;

    @NotBlank(message = "Вкажіть пароль")
    @Size(min = 6, message = "Пароль має містити щонайменше 6 символів")
    private String password;

    @NotNull(message = "Оберіть відділ")
    private Long divisionId;
}
