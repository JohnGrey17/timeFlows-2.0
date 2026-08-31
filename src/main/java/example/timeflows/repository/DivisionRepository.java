package example.timeflows.repository;

import example.timeflows.model.Division;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisionRepository extends JpaRepository<Division, Long> {

    boolean existsByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);

    Optional<Division> findByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);

    @EntityGraph(attributePaths = {"department", "directorate", "manager", "tags"})
    List<Division> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"department", "directorate", "manager", "tags"})
    List<Division> findByDepartmentIdOrderByNameAsc(Long departmentId);

    @EntityGraph(attributePaths = {"department", "directorate", "manager", "tags"})
    List<Division> findByDirectorateIdOrderByNameAsc(Long directorateId);

    @EntityGraph(
            attributePaths = {
                "department",
                "directorate",
                "manager",
                "users",
                "users.roles",
                "subdivisions",
                "tags"
            })
    Optional<Division> findWithDepartmentAndUsersById(Long id);

    Optional<Division> findByManagerId(Long managerId);

    boolean existsByIdAndUsersActiveTrue(Long id);
}
