package example.timeflows.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {

    @NotBlank(message = "Вкажіть поточний пароль")
    private String currentPassword;

    @NotBlank(message = "Вкажіть новий пароль")
    @Size(min = 6, message = "Новий пароль має містити щонайменше 6 символів")
    private String newPassword;

    @NotBlank(message = "Підтвердіть новий пароль")
    private String confirmPassword;
}
