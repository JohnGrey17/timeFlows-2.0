package example.timeflows.repository;

import example.timeflows.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    Optional<User> findByUsername(String username);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    Optional<User> findByEmail(String email);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    Optional<User> findWithDivisionById(Long id);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findAllByOrderByUsernameAsc();

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByActiveTrueOrderByEmailAsc();

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByActiveFalseOrderByEmailAsc();

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "division.manager",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByDivisionIdAndActiveTrueOrderByEmailAsc(Long divisionId);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "division.manager",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByDivisionDepartmentIdAndActiveTrueOrderByEmailAsc(Long departmentId);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByDivisionDirectorateIdAndActiveTrueOrderByEmailAsc(Long directorateId);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findBySubdivisionIdAndActiveTrueOrderByEmailAsc(Long subdivisionId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
