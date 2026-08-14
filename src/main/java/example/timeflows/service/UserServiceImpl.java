package example.timeflows.service;

import example.timeflows.exception.UserException;
import example.timeflows.model.Division;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.UserRepository;
import example.timeflows.controller.dto.RegisterRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            DivisionRepository divisionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Користувача не знайдено"));

        String[] roles = user.getRoles().stream()
                .map(Role::name)
                .toArray(String[]::new);

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isActive())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByActiveTrueOrderByEmailAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findDeactivatedUsers() {
        return userRepository.findByActiveFalseOrderByEmailAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsersByDivision(Long divisionId) {
        return userRepository.findByDivisionIdAndActiveTrueOrderByEmailAsc(divisionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsersByDepartment(Long departmentId) {
        return userRepository.findByDivisionDepartmentIdAndActiveTrueOrderByEmailAsc(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findWithDivisionById(id)
                .orElseThrow(() -> new UserException("Користувача з id " + id + " не знайдено"));
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        User user = new User();
        assertVyriyEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        return create(user, request.getDivisionId());
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("Користувача з email " + email + " не знайдено"));
    }

    @Override
    @Transactional
    public User create(User user, Long divisionId) {
        assertVyriyEmail(user.getEmail());
        user.setUsername(user.getEmail());
        assertUniqueUser(user);
        Division division = findDivision(divisionId);
        user.setDivision(division);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new LinkedHashSet<>(Set.of(Role.EMPLOYEE)));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User update(Long id, User input, Long divisionId) {
        User user = findById(id);
        Division division = findDivision(divisionId);

        assertVyriyEmail(input.getEmail());
        user.setUsername(input.getEmail());
        user.setFirstName(input.getFirstName());
        user.setLastName(input.getLastName());
        user.setEmail(input.getEmail());
        user.setDivision(division);
        if (input.getRoles() != null && !input.getRoles().isEmpty()) {
            user.setRoles(input.getRoles());
        }
        if (input.getPassword() != null && !input.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(input.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserException("Користувача з id " + id + " не знайдено");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deactivate(Long id, String reason) {
        User user = findById(id);
        if (user.getRoles().contains(Role.ADMIN)) {
            throw new UserException("Адміністратора не можна деактивувати");
        }
        user.setActive(false);
        user.setDeactivationReason(reason);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateSalary(Long id, BigDecimal salary) {
        User user = findById(id);
        user.setSalary(salary);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User assignDivisionManager(Long divisionId, Long userId) {
        Division division = findDivision(divisionId);
        User user = findById(userId);
        if (!user.isActive()) {
            throw new UserException("Деактивованого користувача не можна призначити керівником відділу");
        }
        if (!user.getDivision().getId().equals(divisionId)) {
            throw new UserException("Керівник відділу має належати до вибраного відділу");
        }
        User previousManager = division.getManager();
        divisionRepository.findByManagerId(userId)
                .filter(existing -> !existing.getId().equals(divisionId))
                .ifPresent(existing -> {
                    throw new UserException("Користувач вже є керівником іншого відділу");
                });

        division.setManager(user);
        user.getRoles().add(Role.MANAGER);
        divisionRepository.save(division);
        if (previousManager != null && !previousManager.getId().equals(userId)) {
            previousManager.getRoles().remove(Role.MANAGER);
            userRepository.save(previousManager);
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateProfile(String email, String firstName, String lastName) {
        User user = findByEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserException("Поточний пароль вказано невірно");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new UserException("Підтвердження пароля не збігається");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void assertUniqueUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UserException("Користувач з таким email вже існує");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserException("Користувач з таким email вже існує");
        }
    }

    private void assertVyriyEmail(String email) {
        if (email == null || !email.toLowerCase().endsWith("@vyriy.com")) {
            throw new UserException("Будь ласка вкажіть корпоративний email");
        }
    }

    private Division findDivision(Long divisionId) {
        return divisionRepository.findById(divisionId)
                .orElseThrow(() -> new UserException("Відділ з id " + divisionId + " не знайдено"));
    }
}
