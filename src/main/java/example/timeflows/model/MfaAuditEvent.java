package example.timeflows.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mfa_audit_events")
@Getter
@Setter
@NoArgsConstructor
public class MfaAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "target_email", nullable = false)
    private String targetEmail;

    @Column(nullable = false)
    private String action;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
