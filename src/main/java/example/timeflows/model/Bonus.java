package example.timeflows.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bonuses")
@Getter
@Setter
public class Bonus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BonusCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BonusType type = BonusType.MONTHLY;

    @Column(name = "quarter_year")
    private Integer quarterYear;

    @Column(name = "quarter_number")
    private Integer quarterNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BonusStatus status = BonusStatus.PENDING;

    @Column(name = "admin_comment", length = 1000)
    private String adminComment;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
