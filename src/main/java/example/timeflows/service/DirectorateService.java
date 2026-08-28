package example.timeflows.service;

import example.timeflows.model.Directorate;
import java.util.List;

public interface DirectorateService {

    List<Directorate> findAll();

    List<Directorate> findByDepartment(Long departmentId);

    Directorate findById(Long id);

    Directorate create(String name, String description, Long departmentId);

    Directorate update(Long id, String name, String description, Long departmentId);

    void delete(Long id);
}
