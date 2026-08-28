package example.timeflows.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bonus_categories")
@Getter
@Setter
public class BonusCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BonusType type = BonusType.MONTHLY;

    @Column(nullable = false)
    private boolean active = true;
}
