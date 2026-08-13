package example.timeflows.repository;

import example.timeflows.model.Overtime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OvertimeRepository extends JpaRepository<Overtime, Long> {

    @EntityGraph(attributePaths = {"user", "user.roles", "user.division", "user.division.department"})
    List<Overtime> findByUserEmailAndWorkDateBetweenOrderByWorkDateAsc(String email, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"user", "user.roles", "user.division", "user.division.department"})
    List<Overtime> findByUserDivisionIdAndWorkDateBetweenOrderByUserEmailAscWorkDateAsc(Long divisionId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"user", "user.roles", "user.division", "user.division.department"})
    List<Overtime> findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(Long userId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"user", "user.roles", "user.division", "user.division.department"})
    Optional<Overtime> findByIdAndUserEmail(Long id, String email);

    @EntityGraph(attributePaths = {"user", "user.roles", "user.division", "user.division.department"})
    Optional<Overtime> findWithUserById(Long id);

    Optional<Overtime> findByUserEmailAndWorkDate(String email, LocalDate workDate);

    boolean existsByUserEmailAndWorkDate(String email, LocalDate workDate);
}
