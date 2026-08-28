package example.timeflows.repository;

import example.timeflows.model.Directorate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectorateRepository extends JpaRepository<Directorate, Long> {

    boolean existsByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);

    Optional<Directorate> findByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);

    boolean existsByIdAndDivisionsUsersActiveTrue(Long id);

    @EntityGraph(attributePaths = {"department", "manager"})
    List<Directorate> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"department", "manager"})
    List<Directorate> findByDepartmentIdOrderByNameAsc(Long departmentId);
}
