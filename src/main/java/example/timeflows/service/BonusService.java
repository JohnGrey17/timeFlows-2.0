package example.timeflows.service;

import example.timeflows.model.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public interface BonusService {
    List<Bonus> findMonth(YearMonth month);

    List<Bonus> findUserMonth(Long userId, YearMonth month);

    List<Bonus> findDivisionMonth(Long divisionId, YearMonth month);

    Bonus find(Long id);

    Bonus create(
            Long userId,
            Long categoryId,
            BigDecimal amount,
            String description,
            String creatorEmail);

    Bonus create(
            Long userId,
            Long categoryId,
            BonusType type,
            BigDecimal amount,
            String description,
            String creatorEmail);

    Bonus update(
            Long id, Long categoryId, BigDecimal amount, String description, boolean allowFinal);

    void delete(Long id, boolean allowFinal);

    Bonus decide(Long id, BonusStatus status, String comment);

    Bonus decide(Long id, BonusStatus status, String comment, boolean allowFinal);

    List<Bonus> distributeQuarterly(
            int year, int quarter, Long categoryId, Set<Long> userIds, String creatorEmail);

    BigDecimal quarterlyPool(int year, int quarter);

    List<Bonus> findQuarterlyDistribution(int year, int quarter);

    int resetQuarterlyDistribution(int year, int quarter, String actorEmail);

    List<BonusCategory> findCategories();

    BonusCategory createCategory(String name);

    BonusCategory createCategory(String name, BonusType type);

    BonusCategory updateCategory(Long id, String name);

    BonusCategory updateCategory(Long id, String name, BonusType type);

    void deleteCategory(Long id);
}
