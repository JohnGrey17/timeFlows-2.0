package example.timeflows.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OvertimeDecisionRequest {

    @Size(max = 1000, message = "Коментар має містити до 1000 символів")
    private String managerComment;
}
