package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.exception.DepartmentException;
import example.timeflows.model.Department;
import example.timeflows.repository.DepartmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTests {

    @Mock private DepartmentRepository repository;

    private DepartmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepartmentServiceImpl(repository);
    }

    @Test
    void findAllUsesAlphabeticalRepositoryQuery() {
        Department department = department(1L, "Engineering");
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(department));

        assertThat(service.findAll()).containsExactly(department);
    }

    @Test
    void findByIdThrowsWhenDepartmentDoesNotExist() {
        when(repository.findWithDivisionsById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(10L)).isInstanceOf(DepartmentException.class);
    }

    @Test
    void createTrimsAndSavesValidName() {
        Department department = department(null, "  Engineering  ");
        when(repository.save(department)).thenReturn(department);

        assertThat(service.create(department)).isSameAs(department);
        assertThat(department.getName()).isEqualTo("Engineering");
        verify(repository).save(department);
    }

    @Test
    void createRejectsBlankAndDuplicateNames() {
        Department blank = department(null, "  ");
        assertThatThrownBy(() -> service.create(blank)).isInstanceOf(DepartmentException.class);

        Department duplicate = department(null, "Engineering");
        when(repository.existsByNameIgnoreCase("Engineering")).thenReturn(true);
        assertThatThrownBy(() -> service.create(duplicate)).isInstanceOf(DepartmentException.class);
        verify(repository, never()).save(duplicate);
    }

    @Test
    void updateCopiesEditableFields() {
        Department existing = department(1L, "Old");
        Department input = department(null, "New");
        input.setDescription("Description");
        when(repository.findWithDivisionsById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Department result = service.update(1L, input);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("Description");
    }

    @Test
    void deleteChecksExistence() {
        when(repository.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);

        when(repository.existsById(2L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(DepartmentException.class);
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        return department;
    }
}
