package example.timeflows.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Вкажіть email")
    @Email(message = "Вкажіть коректний email")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@vyriy\\.com$",
            message = "Будь ласка вкажіть корпоративний email")
    private String email;

    @NotBlank(message = "Вкажіть пароль")
    private String password;
}
