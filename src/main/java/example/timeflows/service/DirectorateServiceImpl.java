package example.timeflows.service;

import example.timeflows.exception.DepartmentException;
import example.timeflows.model.Department;
import example.timeflows.model.Directorate;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DirectorateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectorateServiceImpl implements DirectorateService {

    private final DirectorateRepository directorates;
    private final DepartmentRepository departments;

    public DirectorateServiceImpl(
            DirectorateRepository directorates, DepartmentRepository departments) {
        this.directorates = directorates;
        this.departments = departments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Directorate> findAll() {
        return directorates.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Directorate> findByDepartment(Long departmentId) {
        return directorates.findByDepartmentIdOrderByNameAsc(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Directorate findById(Long id) {
        return directorates
                .findById(id)
                .orElseThrow(() -> new DepartmentException("Управління не знайдено"));
    }

    @Override
    @Transactional
    public Directorate create(String name, String description, Long departmentId) {
        String normalizedName = requiredName(name, "Назва управління обов'язкова");
        Department department =
                departments
                        .findById(departmentId)
                        .orElseThrow(() -> new DepartmentException("Департамент не знайдено"));
        if (directorates.existsByDepartmentIdAndNameIgnoreCase(departmentId, normalizedName)) {
            throw new DepartmentException("Управління з такою назвою вже існує");
        }
        Directorate directorate = new Directorate();
        directorate.setName(normalizedName);
        directorate.setDescription(trimToNull(description));
        directorate.setDepartment(department);
        return directorates.save(directorate);
    }

    @Override
    @Transactional
    public Directorate update(Long id, String name, String description, Long departmentId) {
        Directorate directorate = findById(id);
        String normalizedName = requiredName(name, "Назва управління обов'язкова");
        Department department =
                departments
                        .findById(departmentId)
                        .orElseThrow(() -> new DepartmentException("Департамент не знайдено"));
        if ((!directorate.getDepartment().getId().equals(departmentId)
                        || !directorate.getName().equalsIgnoreCase(normalizedName))
                && directorates.existsByDepartmentIdAndNameIgnoreCase(
                        departmentId, normalizedName)) {
            throw new DepartmentException("Управління з такою назвою вже існує");
        }
        directorate.setName(normalizedName);
        directorate.setDescription(trimToNull(description));
        directorate.setDepartment(department);
        directorate.getDivisions().forEach(division -> division.setDepartment(department));
        return directorates.save(directorate);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Directorate directorate = findById(id);
        if (!directorate.getDivisions().isEmpty()
                || directorates.existsByIdAndDivisionsUsersActiveTrue(id)) {
            throw new DepartmentException("Не можна видалити управління з активними даними");
        }
        directorates.delete(directorate);
    }

    private String requiredName(String value, String message) {
        if (value == null || value.isBlank()) throw new DepartmentException(message);
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
