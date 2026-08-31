package example.timeflows.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "saved_overtime_filters",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_saved_overtime_filter_owner_name",
                        columnNames = {"owner_id", "name"}))
@Getter
@Setter
public class SavedOvertimeFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "directorate_id")
    private Long directorateId;

    @Column(name = "division_id")
    private Long divisionId;

    @Column(name = "subdivision_id")
    private Long subdivisionId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OvertimeStatus status;

    @Column(name = "filter_year", nullable = false)
    private Integer year;

    @Column(name = "filter_month", nullable = false)
    private Integer month;
}
