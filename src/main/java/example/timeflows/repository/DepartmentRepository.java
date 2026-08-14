package example.timeflows.repository;

import example.timeflows.model.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = {"divisions", "divisions.department", "divisions.manager", "divisions.users", "divisions.users.roles"})
    List<Department> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"divisions", "divisions.manager"})
    Optional<Department> findWithDivisionsById(Long id);
}
