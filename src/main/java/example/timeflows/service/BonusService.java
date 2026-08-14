package example.timeflows.service;

import example.timeflows.model.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface BonusService {
    List<Bonus> findMonth(YearMonth month);
    List<Bonus> findUserMonth(Long userId, YearMonth month);
    List<Bonus> findDivisionMonth(Long divisionId, YearMonth month);
    Bonus find(Long id);
    Bonus create(Long userId, Long categoryId, BigDecimal amount, String description, String creatorEmail);
    Bonus update(Long id, Long categoryId, BigDecimal amount, String description, boolean allowFinal);
    void delete(Long id, boolean allowFinal);
    Bonus decide(Long id, BonusStatus status, String comment);
    List<BonusCategory> findCategories();
    BonusCategory createCategory(String name);
    BonusCategory updateCategory(Long id, String name);
    void deleteCategory(Long id);
}
