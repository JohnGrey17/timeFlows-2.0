package example.timeflows.service;

import example.timeflows.exception.DepartmentException;
import example.timeflows.model.Department;
import example.timeflows.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return departmentRepository.findWithDivisionsById(id)
                .orElseThrow(() -> new DepartmentException("Департамент з id " + id + " не знайдено"));
    }

    @Override
    @Transactional
    public Department create(Department department) {
        return departmentRepository.save(department);
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
