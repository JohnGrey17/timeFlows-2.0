package example.timeflows.service;

import example.timeflows.controller.dto.RegisterRequest;
import example.timeflows.exception.UserException;
import example.timeflows.model.BusinessTag;
import example.timeflows.model.Division;
import example.timeflows.model.Role;
import example.timeflows.model.Subdivision;
import example.timeflows.model.User;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.SubdivisionRepository;
import example.timeflows.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubdivisionRepository subdivisionRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            DivisionRepository divisionRepository,
            SubdivisionRepository subdivisionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.subdivisionRepository = subdivisionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =
                userRepository
                        .findByEmail(username)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("Користувача не знайдено"));

        java.util.Set<String> authorities =
                user.getRoles().stream()
                        .map(role -> "ROLE_" + role.name())
                        .collect(
                                java.util.stream.Collectors.toCollection(
                                        java.util.LinkedHashSet::new));
        if (user.getTags().contains(BusinessTag.SYS_ADMIN)) {
            authorities.add("ROLE_SYS_ADMIN");
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isActive())
                .authorities(authorities.toArray(String[]::new))
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
    public List<User> findActiveUsersByDirectorate(Long directorateId) {
        return userRepository.findByDivisionDirectorateIdAndActiveTrueOrderByEmailAsc(
                directorateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsersBySubdivision(Long subdivisionId) {
        return userRepository.findBySubdivisionIdAndActiveTrueOrderByEmailAsc(subdivisionId);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository
                .findWithDivisionById(id)
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
        User created = create(user, request.getDivisionId());
        if (request.getSubdivisionId() != null) {
            Subdivision subdivision =
                    subdivisionRepository
                            .findById(request.getSubdivisionId())
                            .orElseThrow(() -> new UserException("Підвідділ не знайдено"));
            if (!subdivision.getDivision().getId().equals(request.getDivisionId())) {
                throw new UserException("Підвідділ не належить вибраному відділу");
            }
            created.setSubdivision(subdivision);
            return userRepository.save(created);
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new UserException("Користувача з email " + email + " не знайдено"));
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
            throw new UserException(
                    "Деактивованого користувача не можна призначити керівником відділу");
        }
        if (!user.getDivision().getId().equals(divisionId)) {
            throw new UserException("Керівник відділу має належати до вибраного відділу");
        }
        User previousManager = division.getManager();
        divisionRepository
                .findByManagerId(userId)
                .filter(existing -> !existing.getId().equals(divisionId))
                .ifPresent(
                        existing -> {
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
    public User moveToOrganization(Long userId, Long divisionId, Long subdivisionId) {
        User user = findById(userId);
        Division targetDivision = findDivision(divisionId);
        Division currentDivision = user.getDivision();
        if (currentDivision != null
                && currentDivision.getManager() != null
                && currentDivision.getManager().getId().equals(userId)
                && !currentDivision.getId().equals(divisionId)) {
            throw new UserException("Спочатку призначте іншого керівника поточного відділу");
        }
        Subdivision targetSubdivision = null;
        if (subdivisionId != null) {
            targetSubdivision =
                    subdivisionRepository
                            .findById(subdivisionId)
                            .orElseThrow(() -> new UserException("Підвідділ не знайдено"));
            if (!targetSubdivision.getDivision().getId().equals(divisionId)) {
                throw new UserException("Підвідділ не належить вибраному відділу");
            }
        }
        user.setDivision(targetDivision);
        user.setSubdivision(targetSubdivision);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateRoles(Long userId, Set<Role> roles, String actorEmail) {
        User user = findById(userId);
        User actor = findByEmail(actorEmail);
        Set<Role> requested = roles == null ? Set.of() : new LinkedHashSet<>(roles);
        boolean sysAdmin = actor.getTags().contains(BusinessTag.SYS_ADMIN);
        if (sysAdmin && !actor.getRoles().contains(Role.ADMIN)) {
            boolean changesAdminRole =
                    user.getRoles().contains(Role.ADMIN) != requested.contains(Role.ADMIN);
            if (changesAdminRole) {
                throw new UserException("SYS_ADMIN не може додавати або забирати роль ADMIN");
            }
        }
        if (requested.isEmpty()) {
            throw new UserException("Користувач повинен мати хоча б одну роль");
        }
        if (user.getId().equals(actor.getId())
                && user.getRoles().contains(Role.ADMIN)
                && !requested.contains(Role.ADMIN)) {
            throw new UserException("Адміністратор не може забрати власну роль ADMIN");
        }
        boolean assignedManager =
                user.getDivision() != null
                        && user.getDivision().getManager() != null
                        && user.getDivision().getManager().getId().equals(userId);
        boolean requestsManager = requested.contains(Role.MANAGER);
        if (requestsManager && !assignedManager) {
            if (user.getDivision() == null) {
                throw new UserException("Керівник повинен належати до відділу");
            }
            User previousManager = user.getDivision().getManager();
            divisionRepository
                    .findByManagerId(userId)
                    .filter(existing -> !existing.getId().equals(user.getDivision().getId()))
                    .ifPresent(
                            existing -> {
                                throw new UserException(
                                        "Користувач вже є керівником іншого відділу");
                            });
            user.getDivision().setManager(user);
            divisionRepository.save(user.getDivision());
            if (previousManager != null && !previousManager.getId().equals(userId)) {
                previousManager.getRoles().remove(Role.MANAGER);
                userRepository.save(previousManager);
            }
        } else if (assignedManager && !requestsManager) {
            user.getDivision().setManager(null);
            divisionRepository.save(user.getDivision());
        }
        user.setRoles(requested);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateTags(Long userId, Set<BusinessTag> tags) {
        User user = findById(userId);
        Set<BusinessTag> requested = normalizedTags(tags);
        if (requested.contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
            boolean manager =
                    user.getRoles().contains(Role.MANAGER)
                            && user.getDivision().getManager() != null
                            && user.getDivision().getManager().getId().equals(userId);
            if (!manager) {
                throw new UserException(
                        "PROJECT_MANAGER_LEAD можна призначити лише керівнику відділу");
            }
        }
        user.setTags(requested);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public Division updateDivisionTags(Long divisionId, Set<BusinessTag> tags) {
        Division division = findDivision(divisionId);
        Set<BusinessTag> requested = normalizedTags(tags);
        if (requested.contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
            throw new UserException("PROJECT_MANAGER_LEAD призначається конкретному керівнику");
        }
        division.setTags(requested);
        return divisionRepository.save(division);
    }

    private Set<BusinessTag> normalizedTags(Set<BusinessTag> tags) {
        Set<BusinessTag> requested =
                tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        if (requested.contains(BusinessTag.PROJECT_MANAGER)
                && requested.contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
            throw new UserException("Теги PROJECT_MANAGER і PROJECT_MANAGER_LEAD несумісні");
        }
        return requested;
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
    public void changePassword(
            String email, String currentPassword, String newPassword, String confirmPassword) {
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
        return divisionRepository
                .findById(divisionId)
                .orElseThrow(() -> new UserException("Відділ з id " + divisionId + " не знайдено"));
    }
}
