package example.timeflows.service;

import example.timeflows.model.Division;

import java.util.List;

public interface DivisionService {

    List<Division> findAll();

    List<Division> findByDepartment(Long departmentId);

    Division findById(Long id);

    Division create(Division division, Long departmentId);

    Division update(Long id, Division division, Long departmentId);

    void delete(Long id);
}
