package example.timeflows.service;

import example.timeflows.model.Subdivision;
import java.util.List;

public interface SubdivisionService {

    List<Subdivision> findAll();

    List<Subdivision> findByDivision(Long divisionId);

    Subdivision findById(Long id);

    Subdivision create(String name, String description, Long divisionId);

    Subdivision update(Long id, String name, String description, Long divisionId);

    void delete(Long id);
}
