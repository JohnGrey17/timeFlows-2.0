package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.exception.DivisionException;
import example.timeflows.model.Subdivision;
import example.timeflows.model.User;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.SubdivisionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubdivisionServiceImplTests {

    @Mock private SubdivisionRepository subdivisions;
    @Mock private DivisionRepository divisions;

    private SubdivisionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubdivisionServiceImpl(subdivisions, divisions);
    }

    @Test
    void deleteFlushesSuccessfulRemoval() {
        Subdivision subdivision = subdivision(1L);
        when(subdivisions.findById(1L)).thenReturn(Optional.of(subdivision));

        service.delete(1L);

        verify(subdivisions).delete(subdivision);
        verify(subdivisions).flush();
    }

    @Test
    void deleteRejectsSubdivisionWithDeactivatedUser() {
        Subdivision subdivision = subdivision(1L);
        User deactivatedUser = new User();
        deactivatedUser.setActive(false);
        subdivision.getUsers().add(deactivatedUser);
        when(subdivisions.findById(1L)).thenReturn(Optional.of(subdivision));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(DivisionException.class)
                .hasMessageContaining("деактивованими");
        verify(subdivisions, never()).delete(subdivision);
    }

    private Subdivision subdivision(Long id) {
        Subdivision subdivision = new Subdivision();
        subdivision.setId(id);
        subdivision.setName("Backend");
        return subdivision;
    }
}
