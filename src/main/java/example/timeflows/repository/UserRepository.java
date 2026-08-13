package example.timeflows.repository;

import example.timeflows.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    Optional<User> findWithDivisionById(Long id);

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    List<User> findAllByOrderByUsernameAsc();

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    List<User> findByActiveTrueOrderByEmailAsc();

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    List<User> findByActiveFalseOrderByEmailAsc();

    @EntityGraph(attributePaths = {"division", "division.department", "roles"})
    List<User> findByDivisionIdAndActiveTrueOrderByEmailAsc(Long divisionId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
