package example.timeflows.service;

import example.timeflows.model.Department;

import java.util.List;

public interface DepartmentService {

    List<Department> findAll();

    Department findById(Long id);

    Department create(Department department);

    Department update(Long id, Department department);

    void delete(Long id);
}
