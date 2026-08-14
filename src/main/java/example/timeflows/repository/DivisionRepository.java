package example.timeflows.repository;

import example.timeflows.model.Division;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DivisionRepository extends JpaRepository<Division, Long> {

    boolean existsByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);

    @EntityGraph(attributePaths = {"department"})
    List<Division> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"department", "manager"})
    List<Division> findByDepartmentIdOrderByNameAsc(Long departmentId);

    @EntityGraph(attributePaths = {"department", "manager", "users", "users.roles"})
    Optional<Division> findWithDepartmentAndUsersById(Long id);

    Optional<Division> findByManagerId(Long managerId);
}
