package example.timeflows.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeactivateUserRequest {

    @NotBlank(message = "Причина деактивації обов'язкова")
    @Size(max = 1000, message = "Причина має містити до 1000 символів")
    private String reason;
}
