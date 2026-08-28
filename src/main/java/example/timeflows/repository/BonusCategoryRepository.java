package example.timeflows.repository;

import example.timeflows.model.BonusCategory;
import example.timeflows.model.BonusType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusCategoryRepository extends JpaRepository<BonusCategory, Long> {
    List<BonusCategory> findByActiveTrueOrderByTypeAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByTypeAndNameIgnoreCase(BonusType type, String name);

    boolean existsByTypeAndNameIgnoreCaseAndIdNot(BonusType type, String name, Long id);
}
