package example.timeflows.repository;

import example.timeflows.model.Bonus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BonusRepository extends JpaRepository<Bonus, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "user.division", "user.division.department", "createdBy", "category"})
    Optional<Bonus> findById(Long id);
    @EntityGraph(attributePaths = {"user", "user.division", "user.division.department", "createdBy", "category"})
    List<Bonus> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    @EntityGraph(attributePaths = {"user", "user.division", "user.division.department", "createdBy", "category"})
    List<Bonus> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime from, LocalDateTime to);
    @EntityGraph(attributePaths = {"user", "user.division", "user.division.department", "createdBy", "category"})
    List<Bonus> findByUserDivisionIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long divisionId, LocalDateTime from, LocalDateTime to);
}
