package example.timeflows.repository;

import example.timeflows.model.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    @EntityGraph(
            attributePaths = {
                "divisions",
                "divisions.department",
                "divisions.manager",
                "divisions.users",
                "divisions.users.roles",
                "directorates"
            })
    List<Department> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"divisions", "divisions.manager", "directorates"})
    Optional<Department> findWithDivisionsById(Long id);
}
