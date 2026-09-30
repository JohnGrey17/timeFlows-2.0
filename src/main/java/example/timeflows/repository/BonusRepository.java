package example.timeflows.repository;

import example.timeflows.model.Bonus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusRepository extends JpaRepository<Bonus, Long> {
    @Override
    @EntityGraph(
            attributePaths = {
                "user",
                "user.division",
                "user.division.department",
                "user.division.directorate",
                "user.subdivision",
                "createdBy",
                "category"
            })
    Optional<Bonus> findById(Long id);

    @EntityGraph(
            attributePaths = {
                "user",
                "user.division",
                "user.division.department",
                "user.division.directorate",
                "user.subdivision",
                "createdBy",
                "category"
            })
    List<Bonus> findByAccountingMonthGreaterThanEqualAndAccountingMonthLessThanOrderByCreatedAtDesc(
            LocalDate from, LocalDate to);

    @EntityGraph(
            attributePaths = {
                "user",
                "user.division",
                "user.division.department",
                "user.division.directorate",
                "user.subdivision",
                "createdBy",
                "category"
            })
    List<Bonus>
            findByUserIdAndAccountingMonthGreaterThanEqualAndAccountingMonthLessThanOrderByCreatedAtDesc(
                    Long userId, LocalDate from, LocalDate to);

    @EntityGraph(
            attributePaths = {
                "user",
                "user.division",
                "user.division.department",
                "user.division.directorate",
                "user.subdivision",
                "createdBy",
                "category"
            })
    List<Bonus>
            findByUserDivisionIdAndAccountingMonthGreaterThanEqualAndAccountingMonthLessThanOrderByCreatedAtDesc(
                    Long divisionId, LocalDate from, LocalDate to);

    boolean existsByUserIdAndTypeAndQuarterYearAndQuarterNumber(
            Long userId, example.timeflows.model.BonusType type, Integer year, Integer quarter);

    @EntityGraph(attributePaths = {"user", "createdBy", "category"})
    List<Bonus> findByTypeAndQuarterYearAndQuarterNumberOrderByUserEmailAsc(
            example.timeflows.model.BonusType type, Integer year, Integer quarter);
}
