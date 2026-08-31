package example.timeflows.repository;

import example.timeflows.model.SavedOvertimeFilter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedOvertimeFilterRepository extends JpaRepository<SavedOvertimeFilter, Long> {

    List<SavedOvertimeFilter> findByOwnerIdOrderByNameAsc(Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}
