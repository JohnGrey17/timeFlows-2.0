package example.timeflows.repository;

import example.timeflows.model.MfaAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaAuditEventRepository extends JpaRepository<MfaAuditEvent, Long> {}
