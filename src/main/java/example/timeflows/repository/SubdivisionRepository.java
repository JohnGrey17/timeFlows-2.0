package example.timeflows.repository;

import example.timeflows.model.Subdivision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubdivisionRepository extends JpaRepository<Subdivision, Long> {

    boolean existsByDivisionIdAndNameIgnoreCase(Long divisionId, String name);

    Optional<Subdivision> findByDivisionIdAndNameIgnoreCase(Long divisionId, String name);

    boolean existsByIdAndUsersActiveTrue(Long id);

    @EntityGraph(attributePaths = {"division", "division.department", "division.directorate"})
    List<Subdivision> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"division"})
    List<Subdivision> findByDivisionIdOrderByNameAsc(Long divisionId);
}
