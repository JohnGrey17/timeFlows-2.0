package example.timeflows.repository;

import example.timeflows.model.MfaRecoveryCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {
    List<MfaRecoveryCode> findByUserIdAndUsedAtIsNull(Long userId);

    void deleteByUserId(Long userId);
}
