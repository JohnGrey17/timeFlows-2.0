package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import example.timeflows.controller.dto.RegisterRequest;
import example.timeflows.exception.UserException;
import example.timeflows.model.Division;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {

    @Mock private UserRepository userRepository;
    @Mock private DivisionRepository divisionRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, divisionRepository, passwordEncoder);
    }

    @Test
    void loadUserByUsernameMapsRolesAndActiveState() {
        User user = user(1L, "employee@vyriy.com", Role.EMPLOYEE);
        user.setPassword("encoded");
        user.setActive(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername(user.getEmail());

        assertThat(details.getUsername()).isEqualTo(user.getEmail());
        assertThat(details.isEnabled()).isFalse();
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void loadUserByUsernameRejectsUnknownEmail() {
        when(userRepository.findByEmail("missing@vyriy.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing@vyriy.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void createAssignsDivisionEncodesPasswordAndDefaultsRole() {
        Division division = division(2L);
        User user = user(null, "new@vyriy.com");
        user.setPassword("plain");
        when(divisionRepository.findById(2L)).thenReturn(Optional.of(division));
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);

        User result = service.create(user, 2L);

        assertThat(result.getUsername()).isEqualTo("new@vyriy.com");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getDivision()).isSameAs(division);
        assertThat(result.getRoles()).containsExactly(Role.EMPLOYEE);
    }

    @Test
    void createRejectsExternalAndDuplicateEmails() {
        User external = user(null, "person@example.com");
        assertThatThrownBy(() -> service.create(external, 1L)).isInstanceOf(UserException.class);

        User duplicate = user(null, "person@vyriy.com");
        when(userRepository.existsByUsername("person@vyriy.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(duplicate, 1L)).isInstanceOf(UserException.class);
        verify(userRepository, never()).save(duplicate);
    }

    @Test
    void registerMapsRequestAndUsesCreateWorkflow() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@vyriy.com");
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setPassword("plain-password");
        request.setDivisionId(2L);
        Division division = division(2L);
        when(divisionRepository.findById(2L)).thenReturn(Optional.of(division));
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.register(request);

        assertThat(result.getEmail()).isEqualTo("new@vyriy.com");
        assertThat(result.getFirstName()).isEqualTo("Ada");
        assertThat(result.getLastName()).isEqualTo("Lovelace");
        assertThat(result.getDivision()).isSameAs(division);
    }

    @Test
    void updateKeepsPasswordWhenBlankAndEncodesNewPassword() {
        User existing = user(1L, "old@vyriy.com", Role.EMPLOYEE);
        existing.setPassword("old-encoded");
        Division division = division(2L);
        when(userRepository.findWithDivisionById(1L)).thenReturn(Optional.of(existing));
        when(divisionRepository.findById(2L)).thenReturn(Optional.of(division));
        when(userRepository.save(existing)).thenReturn(existing);

        User blankPassword = user(null, "new@vyriy.com", Role.MANAGER);
        blankPassword.setPassword(" ");
        service.update(1L, blankPassword, 2L);
        assertThat(existing.getPassword()).isEqualTo("old-encoded");

        User newPassword = user(null, "new@vyriy.com", Role.MANAGER);
        newPassword.setPassword("new-password");
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");
        service.update(1L, newPassword, 2L);
        assertThat(existing.getPassword()).isEqualTo("new-encoded");
        assertThat(existing.getRoles()).containsExactly(Role.MANAGER);
    }

    @Test
    void deactivateRejectsAdminAndDeactivatesEmployee() {
        User admin = user(1L, "admin@vyriy.com", Role.ADMIN);
        when(userRepository.findWithDivisionById(1L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.deactivate(1L, "reason"))
                .isInstanceOf(UserException.class);

        User employee = user(2L, "employee@vyriy.com", Role.EMPLOYEE);
        when(userRepository.findWithDivisionById(2L)).thenReturn(Optional.of(employee));
        service.deactivate(2L, "Left company");
        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getDeactivationReason()).isEqualTo("Left company");
        verify(userRepository).save(employee);
    }

    @Test
    void changePasswordValidatesCurrentPasswordAndConfirmation() {
        User user = user(1L, "employee@vyriy.com", Role.EMPLOYEE);
        user.setPassword("old-encoded");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong", "old-encoded")).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword(user.getEmail(), "wrong", "new", "new"))
                .isInstanceOf(UserException.class);

        when(passwordEncoder.matches("old", "old-encoded")).thenReturn(true);
        assertThatThrownBy(() -> service.changePassword(user.getEmail(), "old", "new", "other"))
                .isInstanceOf(UserException.class);

        when(passwordEncoder.encode("new")).thenReturn("new-encoded");
        service.changePassword(user.getEmail(), "old", "new", "new");
        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(userRepository).save(user);
    }

    @Test
    void salaryProfileAndDeleteOperationsPersistChanges() {
        User user = user(1L, "employee@vyriy.com", Role.EMPLOYEE);
        when(userRepository.findWithDivisionById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThat(service.updateSalary(1L, BigDecimal.TEN).getSalary())
                .isEqualByComparingTo(BigDecimal.TEN);
        service.updateProfile(user.getEmail(), "New", "Name");
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name");

        when(userRepository.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void assignDivisionManagerUpdatesRolesAndPreviousManager() {
        Division division = division(2L);
        User candidate = user(1L, "candidate@vyriy.com", Role.EMPLOYEE);
        candidate.setDivision(division);
        User previous = user(3L, "previous@vyriy.com", Role.EMPLOYEE, Role.MANAGER);
        division.setManager(previous);
        when(divisionRepository.findById(2L)).thenReturn(Optional.of(division));
        when(userRepository.findWithDivisionById(1L)).thenReturn(Optional.of(candidate));
        when(divisionRepository.findByManagerId(1L)).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.assignDivisionManager(2L, 1L);

        assertThat(result.getRoles()).contains(Role.MANAGER);
        assertThat(previous.getRoles()).doesNotContain(Role.MANAGER);
        assertThat(division.getManager()).isSameAs(candidate);
        verify(divisionRepository).save(division);
        verify(userRepository).save(previous);
    }

    private User user(Long id, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(email);
        user.setActive(true);
        user.setRoles(new LinkedHashSet<>(Set.of(roles)));
        return user;
    }

    private Division division(Long id) {
        Division division = new Division();
        division.setId(id);
        return division;
    }
}
