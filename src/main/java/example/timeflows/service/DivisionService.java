package example.timeflows.service;

import example.timeflows.model.Division;
import java.util.List;

public interface DivisionService {

    List<Division> findAll();

    List<Division> findByDepartment(Long departmentId);

    List<Division> findByDirectorate(Long directorateId);

    Division findById(Long id);

    Division create(Division division, Long departmentId);

    Division create(String name, Long departmentId);

    Division createInDirectorate(String name, String description, Long directorateId);

    Division update(Long id, Division division, Long departmentId);

    Division updateInDirectorate(Long id, String name, String description, Long directorateId);

    void delete(Long id);
}
