package example.timeflows.service;

import example.timeflows.model.Directorate;
import example.timeflows.model.Division;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.SavedOvertimeFilter;
import example.timeflows.model.Subdivision;
import example.timeflows.model.User;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DirectorateRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.SavedOvertimeFilterRepository;
import example.timeflows.repository.SubdivisionRepository;
import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedOvertimeFilterServiceImpl implements SavedOvertimeFilterService {

    private final SavedOvertimeFilterRepository filters;
    private final UserService userService;
    private final DepartmentRepository departments;
    private final DirectorateRepository directorates;
    private final DivisionRepository divisions;
    private final SubdivisionRepository subdivisions;

    public SavedOvertimeFilterServiceImpl(
            SavedOvertimeFilterRepository filters,
            UserService userService,
            DepartmentRepository departments,
            DirectorateRepository directorates,
            DivisionRepository divisions,
            SubdivisionRepository subdivisions) {
        this.filters = filters;
        this.userService = userService;
        this.departments = departments;
        this.directorates = directorates;
        this.divisions = divisions;
        this.subdivisions = subdivisions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedOvertimeFilter> findForAdmin(String email) {
        User owner = requireAdmin(email);
        return filters.findByOwnerIdOrderByNameAsc(owner.getId());
    }

    @Override
    @Transactional
    public SavedOvertimeFilter save(
            String email,
            String name,
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            OvertimeStatus status,
            Integer year,
            Integer month) {
        User owner = requireAdmin(email);
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 100) {
            throw new IllegalArgumentException("Назва фільтра має містити від 1 до 100 символів");
        }
        if (filters.existsByOwnerIdAndNameIgnoreCase(owner.getId(), normalizedName)) {
            throw new IllegalArgumentException("Фільтр із такою назвою вже існує");
        }
        if (departmentId == null || !departments.existsById(departmentId)) {
            throw new IllegalArgumentException("Оберіть коректний департамент");
        }
        validateHierarchy(departmentId, directorateId, divisionId, subdivisionId);
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Оберіть коректний місяць");
        }
        int currentYear = Year.now().getValue();
        if (year == null || year < currentYear - 10 || year > currentYear + 10) {
            throw new IllegalArgumentException("Оберіть коректний рік");
        }

        SavedOvertimeFilter filter = new SavedOvertimeFilter();
        filter.setOwner(owner);
        filter.setName(normalizedName);
        filter.setDepartmentId(departmentId);
        filter.setDirectorateId(directorateId);
        filter.setDivisionId(divisionId);
        filter.setSubdivisionId(subdivisionId);
        filter.setStatus(status);
        filter.setYear(year);
        filter.setMonth(month);
        return filters.save(filter);
    }

    private User requireAdmin(String email) {
        User user = userService.findByEmail(email);
        if (!user.getRoles().contains(Role.ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Збережені фільтри доступні лише адміністратору");
        }
        return user;
    }

    private void validateHierarchy(
            Long departmentId, Long directorateId, Long divisionId, Long subdivisionId) {
        Directorate directorate = null;
        if (directorateId != null) {
            directorate =
                    directorates
                            .findById(directorateId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Управління не знайдено"));
            if (!directorate.getDepartment().getId().equals(departmentId)) {
                throw new IllegalArgumentException("Управління не належить вибраному департаменту");
            }
        }

        Division division = null;
        if (divisionId != null) {
            division =
                    divisions
                            .findById(divisionId)
                            .orElseThrow(() -> new IllegalArgumentException("Відділ не знайдено"));
            if (!division.getDepartment().getId().equals(departmentId)
                    || (directorateId != null
                            && (division.getDirectorate() == null
                                    || !division.getDirectorate().getId().equals(directorateId)))) {
                throw new IllegalArgumentException("Відділ не відповідає вибраній структурі");
            }
        }

        if (subdivisionId != null) {
            if (division == null) {
                throw new IllegalArgumentException("Для підвідділу потрібно обрати відділ");
            }
            Subdivision subdivision =
                    subdivisions
                            .findById(subdivisionId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Підвідділ не знайдено"));
            if (!subdivision.getDivision().getId().equals(divisionId)) {
                throw new IllegalArgumentException("Підвідділ не належить вибраному відділу");
            }
        }
    }
}
