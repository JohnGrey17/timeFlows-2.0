package example.timeflows.service;

import example.timeflows.controller.dto.PasswordChangeRequest;
import example.timeflows.controller.dto.ProfileRequest;
import java.util.Map;

public interface EmployeePageService {
    Map<String, Object> overtimePage(String email, Integer year, Integer month);

    Map<String, Object> settingsPage(
            String email, ProfileRequest profileRequest, PasswordChangeRequest passwordRequest);
}
