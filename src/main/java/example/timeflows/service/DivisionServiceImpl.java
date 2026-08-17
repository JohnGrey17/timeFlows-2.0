package example.timeflows.service;

import example.timeflows.exception.DepartmentException;
import example.timeflows.exception.DivisionException;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DivisionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DivisionServiceImpl implements DivisionService {

    private final DivisionRepository divisionRepository;
    private final DepartmentRepository departmentRepository;

    public DivisionServiceImpl(
            DivisionRepository divisionRepository, DepartmentRepository departmentRepository) {
        this.divisionRepository = divisionRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Division> findAll() {
        return divisionRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Division> findByDepartment(Long departmentId) {
        return divisionRepository.findByDepartmentIdOrderByNameAsc(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Division findById(Long id) {
        return divisionRepository
                .findWithDepartmentAndUsersById(id)
                .orElseThrow(() -> new DivisionException("Відділ з id " + id + " не знайдено"));
    }

    @Override
    @Transactional
    public Division create(Division division, Long departmentId) {
        if (division.getName() == null || division.getName().isBlank()) {
            throw new DivisionException("Назва підвідділу обов'язкова");
        }
        division.setName(division.getName().trim());
        if (divisionRepository.existsByDepartmentIdAndNameIgnoreCase(
                departmentId, division.getName())) {
            throw new DivisionException(
                    "Підвідділ з такою назвою вже існує у вибраному департаменті");
        }
        division.setDepartment(findDepartment(departmentId));
        return divisionRepository.save(division);
    }

    @Override
    @Transactional
    public Division create(String name, Long departmentId) {
        Division division = new Division();
        division.setName(name == null ? null : name.trim());
        return create(division, departmentId);
    }

    @Override
    @Transactional
    public Division update(Long id, Division input, Long departmentId) {
        Division division = findById(id);
        division.setName(input.getName());
        division.setDepartment(findDepartment(departmentId));
        return divisionRepository.save(division);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!divisionRepository.existsById(id)) {
            throw new DivisionException("Відділ з id " + id + " не знайдено");
        }
        divisionRepository.deleteById(id);
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository
                .findById(departmentId)
                .orElseThrow(
                        () ->
                                new DepartmentException(
                                        "Департамент з id " + departmentId + " не знайдено"));
    }
}
