package example.timeflows.service;

import example.timeflows.model.User;
import java.util.List;

public interface MfaService {
    boolean isRequired(User user);

    String prepare(String email);

    String qrDataUri(String email);

    boolean verify(String email, String code);

    List<String> enable(String email, String code);

    List<String> recoveryCodesForConfirmation(String email);

    void confirmRecoveryCodes(String email);

    boolean useRecoveryCode(String email, String code);

    void disable(String email, String password, String code);

    User user(String email);

    void resetByAdmin(Long userId, String adminEmail);
}
