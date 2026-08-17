package example.timeflows.service;

import example.timeflows.exception.DepartmentException;
import example.timeflows.model.Department;
import example.timeflows.repository.DepartmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Department findById(Long id) {
        return departmentRepository
                .findWithDivisionsById(id)
                .orElseThrow(
                        () -> new DepartmentException("Департамент з id " + id + " не знайдено"));
    }

    @Override
    @Transactional
    public Department create(Department department) {
        if (department.getName() == null || department.getName().isBlank()) {
            throw new DepartmentException("Назва департаменту обов'язкова");
        }
        department.setName(department.getName().trim());
        if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
            throw new DepartmentException("Департамент з такою назвою вже існує");
        }
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department create(String name, String description) {
        Department department = new Department();
        department.setName(name == null ? null : name.trim());
        department.setDescription(description == null ? null : description.trim());
        return create(department);
    }

    @Override
    @Transactional
    public Department update(Long id, Department input) {
        Department department = findById(id);
        department.setName(input.getName());
        department.setDescription(input.getDescription());
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new DepartmentException("Департамент з id " + id + " не знайдено");
        }
        departmentRepository.deleteById(id);
    }
}
