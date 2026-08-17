package example.timeflows.repository;

import example.timeflows.model.BonusCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusCategoryRepository extends JpaRepository<BonusCategory, Long> {
    List<BonusCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
