package example.timeflows.repository;

import example.timeflows.model.BonusCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BonusCategoryRepository extends JpaRepository<BonusCategory, Long> {
    List<BonusCategory> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
