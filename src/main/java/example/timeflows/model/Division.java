package example.timeflows.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "divisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Division {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "division_tags", joinColumns = @JoinColumn(name = "division_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false)
    private Set<BusinessTag> tags = new LinkedHashSet<>();

    /**
     * @deprecated Use {@link #directorate}; kept while legacy records are migrated.
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directorate_id")
    private Directorate directorate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "division", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("username ASC")
    private Set<User> users = new LinkedHashSet<>();

    @OneToMany(mappedBy = "division", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    private Set<Subdivision> subdivisions = new LinkedHashSet<>();
}
