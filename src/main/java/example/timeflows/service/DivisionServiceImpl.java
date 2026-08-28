package example.timeflows.service;

import example.timeflows.exception.DepartmentException;
import example.timeflows.exception.DivisionException;
import example.timeflows.model.Department;
import example.timeflows.model.Directorate;
import example.timeflows.model.Division;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DirectorateRepository;
import example.timeflows.repository.DivisionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DivisionServiceImpl implements DivisionService {

    private final DivisionRepository divisionRepository;
    private final DepartmentRepository departmentRepository;
    private final DirectorateRepository directorateRepository;

    public DivisionServiceImpl(
            DivisionRepository divisionRepository,
            DepartmentRepository departmentRepository,
            DirectorateRepository directorateRepository) {
        this.divisionRepository = divisionRepository;
        this.departmentRepository = departmentRepository;
        this.directorateRepository = directorateRepository;
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
    public List<Division> findByDirectorate(Long directorateId) {
        return divisionRepository.findByDirectorateIdOrderByNameAsc(directorateId);
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
    public Division createInDirectorate(String name, String description, Long directorateId) {
        if (name == null || name.isBlank()) {
            throw new DivisionException("Назва відділу обов'язкова");
        }
        Directorate directorate =
                directorateRepository
                        .findById(directorateId)
                        .orElseThrow(() -> new DivisionException("Управління не знайдено"));
        String normalizedName = name.trim();
        if (divisionRepository.existsByDepartmentIdAndNameIgnoreCase(
                directorate.getDepartment().getId(), normalizedName)) {
            throw new DivisionException("Відділ з такою назвою вже існує в департаменті");
        }
        Division division = new Division();
        division.setName(normalizedName);
        division.setDescription(
                description == null || description.isBlank() ? null : description.trim());
        division.setDepartment(directorate.getDepartment());
        division.setDirectorate(directorate);
        return divisionRepository.save(division);
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
    public Division updateInDirectorate(
            Long id, String name, String description, Long directorateId) {
        Division division = findById(id);
        if (name == null || name.isBlank()) {
            throw new DivisionException("Назва відділу обов'язкова");
        }
        Directorate directorate =
                directorateRepository
                        .findById(directorateId)
                        .orElseThrow(() -> new DivisionException("Управління не знайдено"));
        String normalizedName = name.trim();
        Long departmentId = directorate.getDepartment().getId();
        if ((!division.getDepartment().getId().equals(departmentId)
                        || !division.getName().equalsIgnoreCase(normalizedName))
                && divisionRepository.existsByDepartmentIdAndNameIgnoreCase(
                        departmentId, normalizedName)) {
            throw new DivisionException("Відділ з такою назвою вже існує в департаменті");
        }
        division.setName(normalizedName);
        division.setDescription(
                description == null || description.isBlank() ? null : description.trim());
        division.setDirectorate(directorate);
        division.setDepartment(directorate.getDepartment());
        return divisionRepository.save(division);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Division division = findById(id);
        if (!division.getSubdivisions().isEmpty()
                || divisionRepository.existsByIdAndUsersActiveTrue(id)) {
            throw new DivisionException("Не можна видалити відділ з активними даними");
        }
        divisionRepository.delete(division);
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
