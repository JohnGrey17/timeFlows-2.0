package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.exception.DepartmentException;
import example.timeflows.exception.DivisionException;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DirectorateRepository;
import example.timeflows.repository.DivisionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DivisionServiceImplTests {

    @Mock private DivisionRepository divisionRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DirectorateRepository directorateRepository;

    private DivisionServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new DivisionServiceImpl(
                        divisionRepository, departmentRepository, directorateRepository);
    }

    @Test
    void readMethodsDelegateToRepository() {
        Division division = division(1L, "Platform");
        when(divisionRepository.findAllByOrderByNameAsc()).thenReturn(List.of(division));
        when(divisionRepository.findByDepartmentIdOrderByNameAsc(2L)).thenReturn(List.of(division));
        when(divisionRepository.findWithDepartmentAndUsersById(1L))
                .thenReturn(Optional.of(division));

        assertThat(service.findAll()).containsExactly(division);
        assertThat(service.findByDepartment(2L)).containsExactly(division);
        assertThat(service.findById(1L)).isSameAs(division);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(divisionRepository.findWithDepartmentAndUsersById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(DivisionException.class);
    }

    @Test
    void createTrimsNameAndAssignsDepartment() {
        Department department = department(2L);
        Division division = division(null, "  Platform  ");
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(divisionRepository.save(division)).thenReturn(division);

        Division result = service.create(division, 2L);

        assertThat(result.getName()).isEqualTo("Platform");
        assertThat(result.getDepartment()).isSameAs(department);
    }

    @Test
    void createRejectsInvalidDuplicateAndMissingDepartment() {
        assertThatThrownBy(() -> service.create(division(null, " "), 2L))
                .isInstanceOf(DivisionException.class);

        Division duplicate = division(null, "Platform");
        when(divisionRepository.existsByDepartmentIdAndNameIgnoreCase(2L, "Platform"))
                .thenReturn(true);
        assertThatThrownBy(() -> service.create(duplicate, 2L))
                .isInstanceOf(DivisionException.class);
        verify(divisionRepository, never()).save(duplicate);

        Division missingDepartment = division(null, "QA");
        when(departmentRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(missingDepartment, 3L))
                .isInstanceOf(DepartmentException.class);
    }

    @Test
    void updateAndDeleteHappyPaths() {
        Division existing = division(1L, "Old");
        Department department = department(2L);
        when(divisionRepository.findWithDepartmentAndUsersById(1L))
                .thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(divisionRepository.save(existing)).thenReturn(existing);

        Division result = service.update(1L, division(null, "New"), 2L);
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDepartment()).isSameAs(department);

        service.delete(1L);
        verify(divisionRepository).delete(existing);
    }

    @Test
    void deleteRejectsMissingDivision() {
        when(divisionRepository.findWithDepartmentAndUsersById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DivisionException.class);
    }

    private Division division(Long id, String name) {
        Division division = new Division();
        division.setId(id);
        division.setName(name);
        return division;
    }

    private Department department(Long id) {
        Department department = new Department();
        department.setId(id);
        return department;
    }
}
